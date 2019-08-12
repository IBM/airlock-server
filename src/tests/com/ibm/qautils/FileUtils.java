package tests.com.ibm.qautils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

	
	/**
	 * Return a string representing the given file
	 * @param fileName
	 * @param encoding
	 * @param unifyNewLine - indicate if to remove \r from the end of lines (replace \r\n with \n).
	 * @return
	 * @throws IOException
	 */
	public static String fileToString(String fileName, String encoding, boolean unifyNewLine) throws IOException {
		// create file object
		File file = new File(fileName);
		// create FileInputStream object
		FileInputStream fin = new FileInputStream(file);

		// Create byte array large enough to hold the content of the file. Use File.length to determine size of the file in bytes.
		byte[] fileContent = new byte[(int) file.length()];
		//To read content of the file in byte array, use int read(byte[] byteArray) method of java FileInputStream class.
		fin.read(fileContent);
		fin.close();
		String result = new String(fileContent, encoding);
		if (unifyNewLine) {
			return result.replace("\r\n", "\n");
		}else{
			return result;
		}
	}
	
/**
 * Return a bytes array representing the content of the given file
 * @param file
 * @return
 * @throws IOException
 */
public static byte[] fileToBytes(File file) throws IOException {
		
	    InputStream is = new FileInputStream(file);

	    long length = file.length();
	    byte[] bytes = new byte[(int)length];
	   
	    int offset = 0, num = 0;
	    while ((offset < bytes.length) && ((num=is.read(bytes, offset, bytes.length-offset))) >= 0) {
	        offset += num;
	    }

	    if (offset < bytes.length) {
	    	 is.close();
	    	throw new IOException("The end of the stream has been reached: "+file.getName());
	    }

	    is.close();
	    
	    return bytes;

	}

	
	/**
	 * Split the given file into small files of the given requested size (in bytes).
	 * Reading and splitting is simple. If a binary file is given it will create many corrupted small files.  
	 * @param fileName
	 * @param size - the requested size in bytes. 
	 * @return
	 * @throws IOException 
	 */
	public static ArrayList<File> splitFile(String fileName, int size) throws IOException{
		ArrayList<File> smallFiles = new ArrayList<File>();
		// create file object
		File file = new File(fileName);
		if (size>=file.length()){
			smallFiles.add(file);
			return smallFiles ;
		}
		
		// create FileInputStream object
		FileInputStream fin = new FileInputStream(file);
		int sum = 0 ;
		int index = 0 ;
		while (sum<file.length()){
			byte[] fileContent = new byte[size];
			fin.read(fileContent,0,size);
			sum+=size ;
			File addMe = new File(fileName.substring(0,fileName.lastIndexOf("."))+index+"."+fileName.substring(fileName.lastIndexOf(".")+1)) ;
			addMe.createNewFile() ;
			FileOutputStream fos = new FileOutputStream(addMe);
			fos.write(fileContent);
			fos.close();
			smallFiles.add(addMe);
			index++ ;
		}	
		fin.close();
		return smallFiles ;
	}


	/**
	 * Write the given string to the given file. File is created if not exists.
	 * Previous content is ignored. 
	 * @param writeMe
	 * @param filePath
	 * @throws IOException
	 */
	public static void stringToFile(String writeMe, String filePath) throws IOException{
		File file = new File(filePath);
		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(writeMe);
		fileWriter.flush();
		fileWriter.close();
	}
	
	
	public static void appendStringToEndOfFile(String appendMe, String filePath) throws IOException{
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
	    out.println(appendMe);
	    out.close();
	}
	
	/**
	 * Replace all of the occurrences of the "replaceMe" string with the "toReplace" string
	 * @param filePath
	 * @param replaceMe
	 * @param toReplace
	 * @throws IOException
	 */
	public static void replaceStringInFile(String filePath,String encoding,String replaceMe, String toReplace) throws IOException{
		String fileString = fileToString(filePath, encoding, false);
		fileString = fileString.replaceAll(replaceMe, toReplace);
		stringToFile(fileString, filePath);
	}
	
	/**
	 * Return true if the given string appear in the given file
	 * @param filePath
	 * @param encoding
	 * @param findMe
	 * @return
	 * @throws IOException
	 */
	public static boolean isStringInFile(String filePath, String encoding,String findMe) throws IOException{
		String fileString = fileToString(filePath, encoding, false);
		return fileString.contains(findMe);
	}
	
	/**
	 * Merge all of the files under the given folder into a single file.
	 * The folder is deeply iterated, taking all files also from sub directories.
	 * @param folderPath
	 * @throws IOException 
	 */
	public static void mergeFiles(String folderPath, String outputFileName) throws IOException{
		
		File outputFile = new File(folderPath+File.separator+outputFileName);
		outputFile.createNewFile() ;
		FileWriter writer = new FileWriter(outputFile,true);
		ArrayList<File> files = FolderUtils.allFilesFromFolder(folderPath);
		for (int i=0;i<files.size();i++){
			File current = files.get(i);
			FileInputStream fin = new FileInputStream(current);
			byte[] fileContent = new byte[(int) current.length()];
			//To read content of the file in byte array, use int read(byte[] byteArray) method of java FileInputStream class.
			fin.read(fileContent);
			fin.close();
			writer.write(new String(fileContent)) ;
		}
		writer.close();
	}
	
	/**
	 * Copy the given source file to the requested destination
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copy(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }
	    
	    FileInputStream fis = null ;
	    FileOutputStream fos = null ;
	    FileChannel source = null;
	    FileChannel destination = null;
	    try {
	    	 fis = new FileInputStream(sourceFile) ;
	    	 fos = new FileOutputStream(destFile);
	        source = fis.getChannel();
	        destination = fos.getChannel();

	        long count = 0;
	        long size = source.size();              
	        while((count += destination.transferFrom(source, count, size-count))<size);
	    }
	    finally {
	        if(source != null) source.close();
	        if(destination != null) destination.close();
	        if (fis!=null) fis.close();
	        if (fos!=null) fos.close();
	    }
	}

	/**
	 * Zip the given folder and it's sub folders. 
	 * The zip file will be created under the given folder path with the given folder name with .zip extension. 
	 * @param folderPath
	 * @throws IOException
	 */
	public static void zip(String folderPath) throws IOException{
		
		File folder = new File(folderPath);
		String zipFilePath = folderPath+File.separator+folder.getName()+".zip" ;
		
		ArrayList<File> allFiles = FolderUtils.allFilesFromFolder(folderPath);
		byte[] buffer = new byte[1024];
	    		
	    	FileOutputStream fos = new FileOutputStream(zipFilePath);
	    	ZipOutputStream zos = new ZipOutputStream(fos);
	    			
	    	for(File file : allFiles){
	    			
	    		ZipEntry ze= new ZipEntry(file.getAbsolutePath());
	        	zos.putNextEntry(ze);
	               
	        	FileInputStream in = new FileInputStream(file);
	       	   
	        	int len;
	        	while ((len = in.read(buffer)) > 0) {
	        		zos.write(buffer, 0, len);
	        	}
	               
	        	in.close();
	        	zos.closeEntry();
	    	}
	    	
	    	zos.close();
	    	
	    	
	}

	
	/**
	 * Return the extension of the file (if exist)
	 * @param fileName
	 * @return
	 */
	public static String getExtension (String fileName){
		
		return fileName.substring(fileName.lastIndexOf(".")+1,fileName.length());
	}
	
	
	//TODO find regex in file and copy it into a new file
}
