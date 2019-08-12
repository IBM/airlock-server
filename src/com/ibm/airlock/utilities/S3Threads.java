package com.ibm.airlock.utilities;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.serialize.S3DataSerializer;

public class S3Threads
{
	//------------------------------
	static class S3Thread extends Thread
	{
		String path, data; int filesPerThread;
		S3DataSerializer s3DataSerializer;
		

		S3Thread(String path, String data, int filesPerThread, JSONObject obj) throws Exception
		{
			this.path = path; this.data = data; this.filesPerThread = filesPerThread;
			s3DataSerializer = new S3DataSerializer(obj);
		}
		@Override
		public void run() 
		{
			try {
				for (int i = 0; i < filesPerThread; ++i)
					s3DataSerializer.writeData(path + i, data);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	//------------------------------
	
	public static void main(String[] args)
	{
		if (args.length != 3)
		{
			System.out.println("usage: fileSizeKB filesPerThread numberOfThreads");
			return;
		}

		try {

			run(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void run(int fileSizeKB, int filesPerThread, int numberOfThreads) throws Exception
	{
		
		String data = StringUtils.leftPad("test", fileSizeKB * 1024, '*');
		String envStorgeParams = System.getenv(Constants.ENV_STORAGE_PARAMS);
		JSONObject obj = new JSONObject(envStorgeParams);
		S3Thread[] array = new S3Thread[numberOfThreads];

		for (int i = 0; i < array.length; ++i)
		{
			array[i] = new S3Thread("test/thread" + i, data, filesPerThread, obj);
		}

		System.out.print("file size " + fileSizeKB + "K, " + filesPerThread + " files per thread, " + numberOfThreads + " threads: ");

		long start = System.currentTimeMillis();
		for (int i = 0; i < array.length; ++i)
		{
			array[i].start();
		}
		
		for (int i = 0; i < array.length; ++i)
		{
			array[i].join();
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("finished in " + (end - start) + " milliseconds");
	}

}

