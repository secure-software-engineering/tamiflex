/*******************************************************************************
 * Copyright (c) 2011 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;

public class LogFileDatabase {
	
	protected Map<IProject,Set<File>> projectToLogFiles;
	
	public LogFileDatabase() {
		projectToLogFiles = new HashMap<IProject, Set<File>>();
	}
	
	public void registerLogFile(IProject p, File logFile) {
		if(logFile.exists() && logFile.canRead()) {
			Set<File> files = projectToLogFiles.get(p);	
			if(files==null) {
				files = new HashSet<File>();
				projectToLogFiles.put(p, files);				
			}
			files.add(logFile);
		}		
	}
	
	public List<File> logFilesForProject(IProject p) {
		Set<File> set = projectToLogFiles.get(p);
		ArrayList<File> list = new ArrayList<File>(set);
		Collections.sort(list);
		return list;
	}
	
	public List<File> allLogFiles() {
		Set<File> set = new HashSet<File>();
		for(Set<File> files: projectToLogFiles.values()) {
			set.addAll(files);
		}
		ArrayList<File> list = new ArrayList<File>(set);
		Collections.sort(list);
		return list;
	}

}
