package com.ibm.airlock.admin.serialize;

import java.io.IOException;
import com.ibm.airlock.admin.authentication.UserInfo;

public class AuditLogWriter extends CustomLog
{
	public AuditLogWriter(String logsFolderPath) throws IOException
	{
		super("audit.log", logsFolderPath);
	}

	public void log(String msg, UserInfo userInfo)
	{
		if (userInfo == null) {
			log(msg);
		} else {
			if (userInfo.getApiKey() == null) {
				log(userInfo.getId() + ":	" + msg);
			}
			else {
				log(userInfo.getId() + ", APIkey '" + userInfo.getApiKey() + "':	" + msg);	
			}
			
		}
			
	}
}

/*
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.logging.Logger;

public class AuditLogWriter {
	public static final Logger logger = Logger.getLogger(AuditLogWriter.class.getName());
	
	
	File auditLogFile = null;
	
	public AuditLogWriter() throws IOException {
		//create file if missing
		String path = System.getProperty("catalina.base") + File.separator + "logs" + File.separator + "audit.log";
		auditLogFile = new File(path);
		System.out.println("auditLogFile.getAbsolutePath()=" + auditLogFile.getAbsolutePath());
		if (!auditLogFile.exists()) {
			try {
				auditLogFile.createNewFile();
			} catch (IOException e) {
				//the root folder may not exist. - try creating it
				auditLogFile.getParentFile().mkdirs();
				auditLogFile.createNewFile(); 
			}
		}
	}
	
	public void log(String msg, SecurityFilter.UserInfo userInfo) {
		String userStr = (userInfo == null?"":(userInfo.getId() + ":	"));
		
		BufferedWriter writer = null;
		try {
		    writer = new BufferedWriter(new FileWriter(auditLogFile, true));
		    writer.write((new Date()).toString() + ":	" + userStr + msg + "\n");
		} catch ( IOException e) {
			logger.warning("Fail writing the following audit log: " + msg);
		} finally {
		    try {
		        if ( writer != null)
		        	writer.close( );
		    } catch ( IOException e) {
		    	//ignore
		    }
		}	
	}
}
*/