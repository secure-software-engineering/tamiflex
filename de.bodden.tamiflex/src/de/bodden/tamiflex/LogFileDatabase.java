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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;

public class LogFileDatabase {
	
	private static final class LastModifiedDateComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			return Math.round(f1.lastModified() - f2.lastModified());
		}
	}

	protected Map<IProject,Set<File>> projectToLogFiles;
	
	public LogFileDatabase() {
		projectToLogFiles = new HashMap<IProject, Set<File>>();
	}
	
	public void registerLogFile(IProject p, File logFile) {
		Set<File> files = projectToLogFiles.get(p);	
		if(files==null) {
			files = new HashSet<File>();
			projectToLogFiles.put(p, files);				
		}
		files.add(logFile);
	}
	
	public List<File> logFilesForProject(IProject p) {
		Set<File> set = projectToLogFiles.get(p);
		return sortedList(set);
	}

	public List<File> allLogFiles() {
		Set<File> set = new HashSet<File>();
		for(Set<File> files: projectToLogFiles.values()) {
			set.addAll(files);
		}
		return sortedList(set);
	}

	private List<File> sortedList(Set<File> set) {
		ArrayList<File> list = new ArrayList<File>(set);
		Collections.sort(list, new LastModifiedDateComparator());
		return list;
	}

}
