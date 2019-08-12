package tests.restapi;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.apache.commons.io.IOUtils;

import com.ibm.airlock.admin.serialize.Compression;
import com.ibm.airlock.admin.serialize.Encryption;

import tests.com.ibm.qautils.RestClientUtils;
import tests.com.ibm.qautils.RestClientUtils.RestCallResults;

public class RuntimeRestApi {
	
	public static DateModificationResults getNonEncryptedFileModificationDate(String url, String dateFormat, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.ifModifiedSince(url, dateFormat, sessionToken);
		return new DateModificationResults (res.message, res.code);
	}
	
	public static DateModificationResults getFileModificationDate(String url, String dateFormat, String sessionToken, String seasonID, String baseUrl) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.ifModifiedSince(url, dateFormat, sessionToken);
		if (res.code == 200) {
			SeasonsRestApi s;
			s = new SeasonsRestApi();
			s.setURL(baseUrl);
			try {
				String keyStr = "";
				try {
					keyStr = s.getEncryptionKeyString(seasonID, sessionToken);
				} catch (Exception e) {
					//no encryption for season
					return new DateModificationResults (res.message, res.code);
				}
				
				//read file content as byte[]
				byte[] data = getFileContentAsByteArray(url, sessionToken);
				
				//season runtime data is encrypted
				byte[] key = Encryption.fromString(keyStr);
				
				Encryption e = new Encryption(key);					
				byte[] decrypted = e.decrypt(data);
				String out = Compression.decompress(decrypted);
			     
			    return new DateModificationResults(out, res.code);
			} catch (GeneralSecurityException gse) {
				throw new IOException("GeneralSecurityException: " + gse.getMessage());
			} /*catch (JSONException je) {
				throw new IOException("JSONException: " + je.getMessage());
			}*/
		} 
		else {
			return new DateModificationResults (res.message, res.code);
		}
	}
	
	public static byte[] getFileContentAsByteArray(String url, String sessionToken) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//con.setRequestProperty ("If-Modified-Since", dateFormat);
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
		
		InputStream inp = con.getInputStream(); 
		
		return IOUtils.toByteArray(inp);
	}
	
	public static RestCallResults getRuntimeFile(String url, String sessionToken, String seasonID, String baseUrl) throws IOException{
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
		
		RestCallResults res =  RestClientUtils.buildResult(con);
		if (res.code!=200) {
			return res;
		}
		else {
			SeasonsRestApi s;
			s = new SeasonsRestApi();
			s.setURL(baseUrl);
			try {
				String keyStr = "";
				try { 
					keyStr = s.getEncryptionKeyString(seasonID, sessionToken);
				} catch (Exception e) {
					//no encryption for season
					return res;
				}
				byte[] data = getFileContentAsByteArray(url, sessionToken);
				
				byte[] key = Encryption.fromString(keyStr);
				
				Encryption e = new Encryption(key);					
				byte[] decrypted = e.decrypt(data);
				String out = Compression.decompress(decrypted);
			     
				return new RestCallResults(out, res.code);
			} catch (GeneralSecurityException gse) {
				throw new IOException("GeneralSecurityException: " + gse.getMessage());
			} 
		}
		
	}
	
	public static class DateModificationResults {
		public String message;
		public int code;
		
		public DateModificationResults(String msg, int code) {
			this.message = msg;
			this.code = code;
		}
	} 


}
