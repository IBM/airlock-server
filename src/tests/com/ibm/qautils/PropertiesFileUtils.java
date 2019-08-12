package tests.com.ibm.qautils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PropertiesFileUtils {

	/**
	 * Writes the given key, value pairs into the given file path in a properties file format
	 * (key=value) 
	 * @param properties
	 * @param filePath
	 * @throws IOException
	 */
	public static void propertiesToFile(ArrayList<String[]> properties, String filePath) throws IOException{
		String res = "" ;
		for (int i=0;i<properties.size();i++){
			String[] current = properties.get(i);
			res+=current[0]+"="+current[1]+"\n";
		}
		FileUtils.stringToFile(res, filePath);
	}
	
	/**
	 * Writes the given key, value pairs into the given file path in a properties file format
	 * (key=value) 
	 * @param properties
	 * @param filePath
	 * @throws IOException
	 */
	public static void propertiesToFile(HashMap<String, String> properties, String filePath) throws IOException{
		String res = "" ;
		Set<String> keys = properties.keySet();
		for (String key : keys){
			res+=key+"="+properties.get(key)+"\n" ;
		}
		FileUtils.stringToFile(res, filePath);
	}
	
	/**
	 * Read the given properties file and load the key,value pairs into a HashMap
	 * @param filePath
	 * @return a HashMap with the key,value pairs of the given properties file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> pfileToHashMap(String filePath) throws FileNotFoundException, IOException{
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] keyVal = line.split("=") ;
			map.put(keyVal[0], keyVal[1]);
		}
	 
		br.close();
		return map ;
	}
	
	/**
	 * @param filePath
	 * @return a string with the key value pairs from the given properties file, in a POST request format 
	 * @throws IOException
	 */
	public static String pfileToPostRequestParametersString(String filePath) throws IOException{
		String res = "" ;
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		String line = null;
		while ((line = br.readLine()) != null) {
			res+=line+"&" ;
		}
		br.close();
		return StringUtils.removeLastChar(res);
	}

}
