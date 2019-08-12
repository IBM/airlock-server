package tests.com.ibm.qautils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import com.ibm.airlock.admin.Utilities;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

public class RestClientUtils {
	
	public static class RestCallResults {
		public String message;
		public int code;
		
		public RestCallResults(String msg, int code) {
			this.message = msg;
			this.code = code;
		}
	} 
	
	/**
	 * Return a response string for a POST request.
	 * @param urlString - the url to run against
	 * @param parameters - parameters that should be attached to the request
	 * @return a response string for a POST request
	 * @throws IOException
	 */

	public static RestCallResults sendPost(String urlString, String parameters) throws IOException
	{
		String sessionToken = null;
		return sendPost(urlString, parameters, sessionToken);
	}

	public static RestCallResults sendPostJson(String urlString, String parameters, String sessionToken) throws IOException {
		{
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			//add request header
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestMethod("POST");
			if (sessionToken != null)
				con.setRequestProperty("sessionToken", sessionToken);

			// Send post request
			con.setDoOutput(true);
			con.getOutputStream().write(parameters.getBytes("UTF-8"));
			return buildResult(con);
		}
	}
	public static RestCallResults sendPost(String urlString, String parameters, String sessionToken) throws IOException {
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		//add request header
		con.setRequestMethod("POST");
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);

		// Send post request
		con.setDoOutput(true);
		con.getOutputStream().write(parameters.getBytes("UTF-8"));
		return buildResult(con);
	}

	public static RestCallResults sendPost(String urlString, String parameters, Map<String,String> headers) throws IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		//add request headers
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			 con.setRequestProperty(ent.getKey(), ent.getValue());
		}
		con.setRequestMethod("POST");

	   // Send post request
	   con.setDoOutput(true);
	   con.getOutputStream().write(parameters.getBytes("UTF-8"));
	   return buildResult(con);
	}

	public static RestCallResults sendGet(String url) throws Exception {

		return sendGet(url, null);
	}

	public static RestCallResults sendGet(String url, String sessionToken) throws IOException {
	
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);

		//add request header
		//con.setRequestProperty("User-Agent", USER_AGENT);

		return buildResult(con);
	}

	// this code automatically sends again when the result is a redirect
	public static RestCallResults sendGetWithRedirect(String url, Map<String,String> headers) throws Exception
	{
		CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpGet get = new HttpGet(url);
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			get.setHeader(ent.getKey(), ent.getValue());
		}

		CloseableHttpResponse response = client.execute(get);
		HttpEntity entity = response.getEntity();
		//long len = entity.getContentLength();
		int responseCode = response.getStatusLine().getStatusCode();
		InputStream inp = entity.getContent();
		String out = Utilities.streamToString(inp);

		client.close();
		response.close();

		return new RestCallResults (out, responseCode);
	}


	public static RestCallResults sendPut(String urlString, String parameters) throws IOException
	{
		return sendPut(urlString, parameters, null);
	}

	public static RestCallResults sendPut(String urlString, String parameters, String sessionToken) throws IOException{
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		//add request header
		con.setRequestMethod("PUT");
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
	
		// Send post request
		con.setDoOutput(true);
		con.getOutputStream().write(parameters.getBytes("UTF-8"));
		return buildResult(con);
	}

	public static RestCallResults sendDelete(String urlString) throws Exception {

		return sendDelete(urlString, null);
	}

	public static RestCallResults sendDelete(String urlString, String sessionToken) throws Exception {

		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// optional default is PUT
		con.setRequestMethod("DELETE");
		con.setDoOutput(true);

		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);

		return buildResult(con);
	}

	public static RestCallResults buildResult(HttpURLConnection con)  throws IOException
	{
		int responseCode = con.getResponseCode();
		//System.out.println(con.getHeaderFields());

		InputStream inp;
		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null)
			inp = con.getInputStream(); 
		else
			inp = con.getErrorStream();

		String out;
		if (inp == null)
			out = "Response Code : " + responseCode;
		else
		{
			out = Utilities.streamToString(inp);
//			inp.close();
		}
		return new RestCallResults (out, responseCode);
	}
	
	
	public static RestCallResults ifModifiedSince(String url, String date, String sessionToken) throws IOException{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestProperty ("If-Modified-Since", date);
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
		
		return buildResult(con);


	}

}
