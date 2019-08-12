package tests.com.ibm.qautils;

import java.io.IOException;

public class ArrayUtils {

	/**
	 * Write the given array elements into a csv file 
	 * @param arr
	 * @param filePath
	 * @throws IOException 
	 */
	public static void arrayToCSV(Object[][] arr, String filePath) throws IOException{
		
		StringBuilder sb = new StringBuilder() ;
		for (int row=0;row<arr.length;row++){
			for (int col=0;col<arr[row].length;col++){
				if (col!=(arr[row].length-1)) sb.append(arr[row][col].toString()+",");
				else sb.append(arr[row][col].toString()+"\n") ;
			}
		}
		FileUtils.stringToFile(sb.toString(), filePath);		
	}
	
	public static boolean isIn(String[] arr, String findMe, boolean trim){
		
		boolean in = false ;
		for (int i=0;(i<arr.length && !in );i++){
			if (trim) arr[i] = arr[i].trim();
			if (arr[i].equals(findMe)) in = true ;
		}
		
		return in ;
	}
	
public static int find(String[] arr, String findMe, boolean trim){
		
		int index = -1 ;
		for (int i=0;(i<arr.length && index<0 );i++){
			if (trim) arr[i] = arr[i].trim();
			if (arr[i].equals(findMe)) index = i ;
		}
		
		return index ;
	}
	
}
