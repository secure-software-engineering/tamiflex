package de.bodden.tamiflex.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import de.bodden.tamiflex.db.mysqlaccess.DBController;

public class DBMain {
	
	public static void main(String[] args) throws IOException, NumberFormatException, SQLException {
		if(args.length==0) {
			System.err.println("No log file given.");
			System.exit(1);
		}
		String logFilePath = args[0];
		FileInputStream fis = new FileInputStream(new File(logFilePath));
		BufferedReader in = new BufferedReader(new InputStreamReader(fis));
		String line;
		while((line=in.readLine())!=null) {
			DBController.insert(line);
		}

		String url = System.getProperty("TFDB_URL","jdbc:mysql://127.0.0.1:3306/");
		String username = System.getProperty("TFDB_USER","root");
		String password = System.getProperty("TFDB_PW","");
		
		if(DBController.connect(true, url, username, password)) {
			DBController.sendBuffer();
			DBController.closeDB();
		} else {
			System.err.println("Could not connect to database at "+url);
		}
	}

}
