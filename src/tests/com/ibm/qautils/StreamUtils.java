package tests.com.ibm.qautils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamUtils {

	/**
	 * Write the given input stream into the given file (full path is expected).
	 * @param stream
	 * @param fileName
	 * @throws IOException
	 */
	public static void istreamToFile(InputStream stream, String fileName) throws IOException {

		byte[] buf = new byte[1024];
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileName);
			int numRead;
			while ((numRead = stream.read(buf))>0) {
				fos.write(buf,  0, numRead);
			}
		} finally {
			if (fos!=null) {
				fos.close();
			}
		}
	}


	/**
	 * Return a string representing the given input stream content.
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static String istreamToString(InputStream stream) throws IOException{

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(stream));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}

		return sb.toString();
	}

	/**
	 * Copy the given source input stream into the given destination output stream.
	 * Read in chunks of 1024 bytes.
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void istreamToOstream(InputStream source, OutputStream destination) throws IOException {

		if (source == null) {
			return;
		}

		if (source.markSupported()){
			source.reset();
		}	

		byte[] buffer = new byte[1024];
		boolean moreToCopy = true;
		while (moreToCopy) {			
			int amountRead = source.read(buffer);
			if (amountRead == -1) {
				moreToCopy = false;
			}
			else {
				destination.write(buffer, 0, amountRead);
			}
		}
		destination.flush();

		if (source.markSupported()){
			source.reset();
		}		
	}

}
