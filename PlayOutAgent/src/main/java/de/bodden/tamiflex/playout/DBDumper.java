package de.bodden.tamiflex.playout;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;

public class DBDumper {

	public static void dumpFileToDatabase(File jarfile, File logFile) {		
		Project project = new Project();
        project.setBaseDir(jarfile.getParentFile());
        project.init();
        Java javaTask = new Java();
        javaTask.setTaskName("runjava");
        javaTask.setProject(project);
        javaTask.setFork(true);
        javaTask.setSpawn(true);
        javaTask.setJar(jarfile);        
        javaTask.setArgs(logFile.getAbsolutePath());
        javaTask.init();
        javaTask.executeJava();
	}

}
