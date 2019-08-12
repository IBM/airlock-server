package com.ibm.airlock.admin.serialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class MultiThreadIO
{
	static final Logger logger = Logger.getLogger(MultiThreadIO.class.getName());

	DataSerializer ds;
	ExecutorService pool;
	ArrayList<Future<Runner>> reading = new ArrayList<Future<Runner>>();
	ArrayList<Future<Runner>> writing = new ArrayList<Future<Runner>>();
	ArrayList<Future<Runner>> backups = new ArrayList<Future<Runner>>();

	enum Mode { READ, WRITE, BACKUP };

	//---------------------------------------
	class Runner implements Callable<Runner>
	{
		Mode mode;
		String path, data;
		String error = null;

		// TODO: accept bytes for encrypted files
		Runner(Mode mode, String path, String data)
		{
			this.mode = mode;
			this.path = path;
			this.data = data; // in read mode this is null. in backup mode, it contains the new path
		}
		@Override
		public Runner call() throws Exception
		{
			try {
				switch (mode)
				{
				case READ:	data = ds.readDataToString(path); break;
				case WRITE:	ds.writeData(path, data); break;
				case BACKUP: break; 
				default:
				}
			}
			catch (Exception e)
			{
				error = "IO error: " + e.toString();
			}
			return this;
		}
	}

	//---------------------------------------
	public MultiThreadIO(DataSerializer ds, int maxThread)
	{
		this.ds = ds;
		this.pool = Executors.newFixedThreadPool(maxThread);
	}
	public void shutdown()
	{
		if (reading.isEmpty() && writing.isEmpty() && backups.isEmpty())
		{}
		else
			logger.warning("MultiThread IO is shut down before all operations have completed");

		pool.shutdown();
	}

	//------------------------------------
	public synchronized void addWrite(String path, String data) throws IOException
	{
		Runner callable = new Runner(Mode.WRITE, path, data);
		Future<Runner> future = pool.submit(callable);
		writing.add(future);
	}
	public synchronized void endWrites() throws IOException
	{
		ArrayList<String> errors = new ArrayList<String>();
		for (Future<Runner> item : writing)
		{
			try {
				Runner runner = item.get();
				if (runner.error != null)
					errors.add(runner.path + ": " + runner.error);
			}
			catch (Exception e)
			{
				errors.add(e.toString());
			}
		}

		writing.clear();
		if (!errors.isEmpty())
			throw new IOException("errors during writing: " + errors.toString());
	}

	//------------------------------------
	public synchronized void addRead(String path) throws IOException
	{
		Runner callable = new Runner(Mode.READ, path, null);
		Future<Runner> future = pool.submit(callable);
		reading.add(future);
	}
	// return path/data map. TODO: enforce unique path?
	public synchronized Map<String,String> endReads() throws IOException
	{
		Map<String,String> out = new HashMap<String,String>();
		ArrayList<String> errors = new ArrayList<String>();

		for (Future<Runner> item : reading)
		{
			try {
				Runner runner = item.get();
				if (runner.error == null)
					out.put(runner.path, runner.data);
				else
					errors.add(runner.path + ": " + runner.error);
			}
			catch (Exception e)
			{
				errors.add(e.toString());
			}
		}

		reading.clear();
		if (!errors.isEmpty())
			throw new IOException("errors during reading: " + errors.toString());

		return out;
	}

	//------------------------------------
	public synchronized void addBackup(String path, String newPath) throws IOException
	{
		Runner callable = new Runner(Mode.BACKUP, path, newPath);
		Future<Runner> future = pool.submit(callable);
		backups.add(future);
	}
	public synchronized void endBackups() throws IOException
	{
		ArrayList<String> errors = new ArrayList<String>();
		for (Future<Runner> item : backups)
		{
			try {
				Runner runner = item.get();
				if (runner.error != null)
					errors.add(runner.path + ": " + runner.error);
			}
			catch (Exception e)
			{
				errors.add(e.toString());
			}
		}

		backups.clear();
		if (!errors.isEmpty())
			throw new IOException("errors during backups: " + errors.toString());
	}
}
