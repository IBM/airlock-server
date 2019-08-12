package tests.com.ibm.qautils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class FolderUtils {
	
		
	/**
	 * This method iterates the given folder path and test if it fit the expected given structure.
	 * The structure is a map of (string, array list of strings) pairs: 
	 * The string key is the name of the folder or a sub folder in it
	 * The array list of strings is the content of that folder (files or sub folders, not a deep description but the zero level items of that folder).
	 * So, if a folder contains a sub folder than this sub folder will appear in the list of the parent folder values and as additional key in the map with it's own list of content items. 
	 * @param folderPath
	 * @param expectedStructure
	 * @throws Exception
	 */
	public static void validateFolderStructure(String folderPath, HashMap<String, ArrayList<String>> expectedStructure) throws Exception{
		
		HashMap<String, ArrayList<String>> folderStructure = buildFolderStructure(folderPath);
		Set<String> expectedItems = expectedStructure.keySet();
			for(String itemName : expectedItems){
				if (!folderStructure.containsKey(itemName)) throw new Exception("An expected item in the given structure wasn't found. Item name: "+itemName);
				ArrayList<String> folderValue = folderStructure.get(itemName);
				ArrayList<String> expectedValue = expectedStructure.get(itemName);
				if (!expectedValue.containsAll(folderValue)) throw new Exception("Unexpected items were found in folder: "+itemName);
				if (!folderValue.containsAll(expectedValue)) throw new Exception("Expected items are missing in folder: "+itemName);
			} 
	}
	
	/**
	 * Return a map representing the structure of the given folder.
	 * The structure is a map of (string, array list of strings) pairs: 
	 * The string key is the name of the folder or a sub folder in it
	 * The array list of strings is the content of that folder (files or sub folders, not a deep description but the zero level items of that folder).
	 * So, if a folder contains a sub folder than this sub folder will appear in the list of the parent folder values and as additional key in the map with it's own list of content items. 
	 * @param folderPath
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> buildFolderStructure(String folderPath){
		return mapContent(new File(folderPath), new HashMap<String, ArrayList<String>>());
	}
	
	private static HashMap<String, ArrayList<String>> mapContent(File item, HashMap<String, ArrayList<String>> map){
		if (item.isFile()) return map ;
		File[] content = item.listFiles() ;
		ArrayList<String> contentNames = new ArrayList<String>();
		for (int i=0;i<content.length;i++){
			if (content[i].isDirectory()) map = mapContent(content[i], map);
			contentNames.add(content[i].getName()) ;
		}
		map.put(item.getName(), contentNames);
		return map ;
	}
	
	/**
	 * Return a list of all of the files in that directory. Useful when you have a directory tree 
	 * with many sub directories and you want to get all of the files, ignoring the tree structure.
	 * @param dir
	 * @param list
	 * @return
	 */
	private static ArrayList<File> allFilesFromDirectory(File dir, ArrayList<File> list) {
		dir.setReadOnly();
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()){
				if (!(list.contains(files[i])))
				{
					list.add(files[i]);
				}
			}
			if (files[i].isDirectory())
				allFilesFromDirectory(files[i], list);
		}
		dir.setWritable(true);
		return list;
	}
	
	public static ArrayList<File> allFilesFromFolder(String folderPath){
		return allFilesFromDirectory(new File(folderPath), new ArrayList<File>());
	}
	
	/**
	 * Return a HashMap representing the given files array: a file name is mapped to file object
	 * @param content
	 * @return
	 */
	public static HashMap<String, File> folderContentArrayToHashMap(File[] content){
		HashMap<String, File> map = new HashMap<String, File>() ;
			for (int i=0;i<content.length;i++)
				map.put(content[i].getName(), content[i]) ;
		return map ;
	}
	
	/**
	 * Return a HashMap representing the given files array: a file name is mapped to file object
	 * @param content
	 * @return
	 */
	public static HashMap<String, File> folderContentArrayToHashMap(ArrayList<File> content){
		HashMap<String, File> map = new HashMap<String, File>() ;
			for (int i=0;i<content.size();i++){
				File file = content.get(i);
				map.put(file.getName(), file) ;
			}
		return map ;
	}
	
	/**
	 * Return true if the given folder is empty and false otherwise
	 * @param folderPath
	 * @return
	 * @throws Exception if the given folder path is not a folder
	 */
	public static boolean isEmptyFolder(String folderPath) throws Exception{
		
		File file = new File(folderPath);
		
		if(file.isDirectory()){
			String[] content = file.list() ; 
			if (content!=null){
				if(content.length>0) return false ;
				else return true ;
			}else throw new Exception("A NULL value was returned from File list method. The given path is not a folder");				
		}else throw new Exception("The given path is not a folder");
	}
	
	/**
	 * Validate the given path as a data folder: it should exist, should be a folder and should be readable
	 * @param dataPath
	 * @throws Exception
	 */
	public static void validateDataFolder(String dataPath) throws Exception{
		File dataFolder = new File(dataPath);
		if (!dataFolder.exists()) throw new Exception("The given folder path does not exist");
		if (!dataFolder.isDirectory()) throw new Exception("The given folder path is not a folder");
		if (!dataFolder.canRead()) throw new Exception("The given folder path cannot be read");
	}
	
	//TODO
	/*
	 * merge two folders
	 * 
	 */

}
