package tests.com.ibm.qautils;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class HttpClientUtils {

	
	public static String sendPost(String urlString, String parameters) throws IOException{
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
	
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(parameters);
		wr.flush();
		wr.close();

		String response = ""  ;
		int responseCode = con.getResponseCode();
		response+="Sending 'POST' request to URL : " + url.toString();
		response+="\nResponse Code : " + responseCode;

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer res = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			res.append(inputLine);
		}
		in.close();
		
		if (res.length()>0) response+="\n"+res.toString();
		return response ;
	}
}
