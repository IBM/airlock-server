package com.ibm.airlock.admin.serialize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {
	public static byte[] compress(String data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data.getBytes());
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return compressed;
	}
	
	public static String decompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = new GZIPInputStream(bis);
		BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		gis.close();
		bis.close();
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		try {
			   String skey = "Bar12345Bar12345"; // 128 bit key
		       String plaintext = "lalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalalala";
		       System.out.println("original: " + plaintext);

		       byte[] key = Encryption.fromString(skey);
		       Encryption e = new Encryption(key);
		       
		       byte[] compressed = Compression.compress(plaintext);
		       byte[] encrypted = e.encrypt(compressed);
		       
		       
		       byte[] decrypted = e.decrypt(encrypted);
		       String out = Compression.decompress(decrypted);
		       
		       System.out.println("uncompressed: " + out);		        
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
