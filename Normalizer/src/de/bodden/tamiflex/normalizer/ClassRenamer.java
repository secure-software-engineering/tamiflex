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
package de.bodden.tamiflex.normalizer;

import static de.bodden.tamiflex.normalizer.Hasher.dotted;
import static de.bodden.tamiflex.normalizer.Hasher.slashed;

import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

/**
 * Provides functionality to rename references to generated classes, including fully-qualified
 * type names in String constants. 
 */
public class ClassRenamer {
	
	/**
	 * Exception that is thrown if the from/to map contains no entry for the generated class
	 * with name <code>typeName</code>.
	 */
	@SuppressWarnings("serial")
	public static class NoHashedNameException extends RuntimeException{
		public NoHashedNameException(String typeName) {
			super(typeName);
		}
	}

	/**
	 * Renames references to generated classes according to the mapping <code>fromTo</code>
	 * in the bytecode <code>classBytes</code>.
	 * @param fromTo This map must contain, for every generated class c an entry that maps c to
	 * 		some other valid class name. Generated classes are such classes whose name is matched by
	 * 		{@link Hasher#containsGeneratedClassName(String)}.
	 * @param classBytes The bytecode in which the renaming should take place. This array wil remain
	 * 		unmodified.
	 * @return The bytecode containing the renamed references.
	 */
	public static byte[] replaceClassNamesInBytes(final Map<String, String> fromTo,	byte[] classBytes) {
		ClassReader creader = new ClassReader(classBytes);
    	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    	RemappingClassAdapter visitor = new RemappingClassAdapter(writer,new Remapper(){
    		//rename a type reference
    		@Override
    		public String map(String typeName) {
    			String newName = fromTo.get(typeName);
    			if(Hasher.containsGeneratedClassName(typeName) && newName==null) {
    				throw new NoHashedNameException(typeName);
    			}
    			if(newName!=null) typeName = newName;
    			return super.map(typeName);
    		}
    	}) {
    		//visit the body of the method
    		@Override
    		public MethodVisitor visitMethod(int access, String name,
    				String desc, String signature, String[] exceptions) {
    			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    			mv = new RemappingStringConstantAdapter(mv, new StringRemapper() {
    				//rename any string constants
    				@Override
    				public String remapStringConstant(String constant) {
    					//string constants will refer to the type using a dotted name; replace dots by slashes... 
    					String slashed = slashed(constant);
						String to = fromTo.get(slashed);
    	    			if(Hasher.containsGeneratedClassName(slashed) && to==null) {
    	    				throw new NoHashedNameException(slashed);
    	    			}
    					if(to!=null) constant = dotted(to);
    					return super.remapStringConstant(constant);
    				}
    			});
				return mv;
    		}
    	};
    	creader.accept(visitor, 0);
        return writer.toByteArray();
	}
}
