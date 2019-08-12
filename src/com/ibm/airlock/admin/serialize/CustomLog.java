package com.ibm.airlock.admin.serialize;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLog
{
	Logger logger;

	public static final String DEFAULT_LOG_FOLDER_PATH = "DEFAULT_LOG_FOLDER_PATH";

	public CustomLog(String fileName, int maxFileSz, int generations, String logsFolderPath) throws IOException
	{
		init(fileName, maxFileSz, generations, logsFolderPath);
	}
	public CustomLog(String fileName, String logsFolderPath) throws IOException
	{
		init(fileName, 1024 * 1024, 1000, logsFolderPath);
	}
	void init(String fileName, int maxFileSz, int generations, String logsFolderPath) throws IOException
	{
		String path;
		if (logsFolderPath.equals(DEFAULT_LOG_FOLDER_PATH)) //service is running on AWS
			path = System.getProperty("catalina.base") + File.separator + "logs" + File.separator + fileName;
		else //service is running in AZURE
			path = logsFolderPath + File.separator + fileName;
		
		//String path = "D:" + File.separator + "home" + File.separator + "logFiles" + File.separator + fileName;
		
		File file = new File(path);
		System.out.println("Logger at " + file.getAbsolutePath());

		if (!file.exists())
			file.getParentFile().mkdirs(); // just in case

		FileHandler fileHandler = new FileHandler(path, maxFileSz, generations, true);
		fileHandler.setEncoding("UTF-8");
	    fileHandler.setFormatter(new CustomFormatter());

	    logger = Logger.getLogger(CustomLog.class.getName() + "_" + fileName);
	    logger.setUseParentHandlers(false);
	    logger.addHandler(fileHandler);
	    logger.setLevel(Level.INFO);
	}
	public void log(String msg)
	{
		logger.info(msg);
	}
	public void fine(String msg)
	{
		logger.fine(msg);
	}
	public void setFine(boolean on)  // using just two levels, info and fine
	{
		logger.setLevel(on ? Level.FINE : Level.INFO);
		logger.info("setting fine logging " + (on ? "on" : "off"));
	}
	public boolean isFine()
	{
		return logger.getLevel() == Level.FINE;
	}
	public void close()
	{
	}

	static class CustomFormatter extends Formatter
	{
		@Override
	    public String format(LogRecord record)
		{
			return (new Date().toString()) + ":	" + record.getMessage() + "\n";
		}
	}
}
