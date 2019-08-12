package tests.com.ibm.qautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtils {
	
	private static final String TASKS = "tasklist";
	private static final String TASKILL = "taskkill /IM ";
	
	/**
	 * Return a list of the current running processes
	 * @return
	 * @throws IOException
	 */
	public static List<String> getProcesses() throws IOException {   
		
		List<String> list=new ArrayList<String>();
		Process p = Runtime.getRuntime().exec(TASKS);  
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())); 
		String line=new String();
		while ((line = reader.readLine()) != null)
		{
			if (!(line.startsWith("tasklist.exe")||line.startsWith("Image Name")))
			{
				if (line.contains("  "))
				{
					int pos = line.indexOf("  ");
					String l= line.substring(0,pos);
					list.add(l);
				}
			}
		}
		return list;  
	}
	
	/**
	 * Return true if the given process is running
	 * @param servicename
	 * @return
	 * @throws IOException
	 */
	public static boolean isProcessRunning(String servicename) throws IOException {
		
		List<String> list=getProcesses();
		if (list.contains(servicename))
			return true;
		else
			return false;
	}
	
	/**
	 * Kill the given process 
	 * @param servicename
	 * @throws IOException
	 */
	public static void killProcess(String servicename) throws IOException {   
		Runtime.getRuntime().exec(TASKILL + servicename);
	}

	/**
	 * Execute the given commands and return the standard output as a string
	 * @param commands
	 * @return
	 * @throws IOException
	 */
	public static String execCommands(String[] commands) throws IOException{
		String res = "" ;
		Process proc = Runtime.getRuntime().exec(commands);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

		String s = null;
		while ((s = stdInput.readLine()) != null) {
		    res+=s;
		}

		return res;
	}

}
