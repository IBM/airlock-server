package com.ibm.airlock.admin.serialize;

import java.io.IOException;
import java.util.ArrayList;

public class TranslatorLogWriter extends CustomLog
{
	ArrayList<String> tracer = null;

	public TranslatorLogWriter(String logsFolderPath) throws IOException
	{
		super("translator.log", logsFolderPath);
	}

	@Override
	public void log(String msg)
	{
		super.log(msg);
		if (tracer != null)
			addTrace(msg);
	}
	@Override
	public void fine(String msg)
	{
		if (isFine())
		{
			super.fine(msg);
			if (tracer != null)
				addTrace(msg);
		}
	}

	public synchronized void setTrace(boolean on)
	{
		if (on)
			tracer = new ArrayList<String>();
		else
			tracer = null;
	}
	synchronized void addTrace(String msg)
	{
		if (tracer != null) // double checked after locking
			tracer.add(msg);
	}
	public synchronized ArrayList<String> getTrace()
	{
		if (tracer == null)
			return new ArrayList<String>();

		ArrayList<String> out = tracer;
		tracer = new ArrayList<String>();
		return out;
	}
}

/*
public class SmartlingLogWriter
{
	static final String FILE_NAME = "smartling.log";
	static Logger  logger = Logger.getLogger(SmartlingLogWriter.class.getName());

	static Logger log4j = Logger.getLogger(SmartlingLogWriter.class.getName()); // this one ignores the utf-8 encoding in tomcat
	OutputStreamWriter writer = null; // this one does not roll files

	public SmartlingLogWriter() throws IOException
	{
		String path = System.getProperty("catalina.base") + File.separator + "logs" + File.separator + FILE_NAME;
		File file = new File(path);
		System.out.println("smartlingLogFile.getAbsolutePath()=" + file.getAbsolutePath());

		if (!file.exists())
			file.getParentFile().mkdirs(); // just in case

		int maxFileSz = 1024 * 1024;
		int generations = 100;
		FileHandler fileHandler = new FileHandler(path, maxFileSz, generations, true);

		fileHandler.setEncoding("UTF-8");
	    fileHandler.setFormatter(new CustomFormatter());

//	    for (Handler h : logger.getHandlers()) {logger.removeHandler(h); }
	    logger.setUseParentHandlers(false);
	    logger.addHandler(fileHandler);


		//log4j.removeAllAppenders();
		//RollingFileAppender app = new RollingFileAppender(new PatternLayout("%d{ISO8601} %m%n"), file.getAbsolutePath(), true);
		//app.setEncoding("UTF-8"); // this worked in the application but not in tomcat
		//log4j.addAppender(app);

		//writer = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
	}

	public void log(String msg)
	{
		logger.info(msg);
		//log4j.info(msg);

		try {
		    writer.write((new Date()).toString() + ":	" + msg + "\n");
		}
		catch ( IOException e) {
			System.out.println("Failed to write the following smartling log: " + msg);
		}
		finally {
		    try { writer.flush(); }
		    catch ( IOException e) {}
		}

	}

	public void close()
	{
//		log4j.shutdown();

//	    try { if (writer != null) writer.close(); }
//	    catch ( IOException e) {}
	}

	static class CustomFormatter extends Formatter
	{
		@Override
	    public String format(LogRecord record)
		{
			return (new Date().toString()) + ":	" + record.getMessage() + "\n";
		}

	     
	    private static final String format = "%1$tc: %5$s%n"; // just the date and the message
	    private final Date dat = new Date();

		@Override
	    public synchronized String format(LogRecord record)
		{
	        dat.setTime(record.getMillis());
	        String source = "";
	        String message = formatMessage(record);
	        String throwable = "";

	        return String.format(format,
	                             dat,
	                             source,
	                             record.getLoggerName(),
	                             record.getLevel(),
	                             message,
	                             throwable);
	    } 
	}
}
*/