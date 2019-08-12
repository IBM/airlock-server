package tests.com.ibm.qautils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVUtils {
	
	public static ArrayList<String[]> readCSVRecords(String filePath, String separator) throws IOException{
		ArrayList<String[]> res = new ArrayList<String[]>() ;
		File csvFile = new File(filePath) ;
		BufferedReader reader = new BufferedReader(new FileReader(csvFile));
		String line = null;
		while ((line = reader.readLine()) != null) {
			res.add(line.split(separator));
		}
		
		reader.close();
		
		return res ;
	}
	
	public static ArrayList<String[]> readCSVRecordsSkipComments(String filePath, String separator, String commentSign) throws IOException{
		ArrayList<String[]> res = new ArrayList<String[]>() ;
		File csvFile = new File(filePath) ;
		BufferedReader reader = new BufferedReader(new FileReader(csvFile));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith(commentSign)) res.add(line.split(separator));
		}
		reader.close();
		return res ;
	}
	
	public static ArrayList<String> readCSVColumn(String filePath, String separator, int columnIndex) throws IOException{
		ArrayList<String> res = new ArrayList<String>() ;
		File csvFile = new File(filePath) ;
		BufferedReader reader = new BufferedReader(new FileReader(csvFile));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(separator);
			res.add(values[columnIndex]);
		}

		reader.close();
		return res ;
	}
	
	public static void appendRecordToCSVFile(String[] values, String separator, String filePath) throws IOException{
		
		String res = "" ;
		for (int i=0;i<values.length;i++)
			if (i!=(values.length-1)) res+= values[i]+separator ;
			else res+=values[i];
		
		FileUtils.appendStringToEndOfFile(res, filePath);
	}

}
