package tests.restapi.testdriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

//import org.apache.commons.codec.binary.Base64;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;

import javax.xml.bind.DatatypeConverter;

public class ConnectToSso
{
	//-----------------------------------------------------
	@SuppressWarnings("serial")
	static class TrimProperies extends Properties
	{
		public String get(String key) throws Exception
		{
			String out = super.getProperty(key);
			if (out == null)
				throw new Exception("missing property " + key);
			return out.trim();
		}
	}
	public static class RestCallResults
	{
		public int code;
		public String message;
		Map<String, List<String>> headers;

		RestCallResults(String message, int code, Map<String, List<String>> headers)
		{
			this.message = message; this.code = code; this.headers = headers;
		}
	}
	//-----------------------------------------------------

	String client_id, client_secret, authorization_url, token_url, issuer_id, redirect_uri;

	public static void main(String[] args)
	{
		if (args.length != 3)
		{
			System.out.println("usage: ConnectToSso configPath user password");
			return;
		}
		try {
			ConnectToSso sso = new ConnectToSso(args[0]);
			String jwt = sso.getJWT(args[1], args[2]);
			System.out.println("sso jwt:   " + jwt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public ConnectToSso(String configPath) throws Exception
	{
		TrimProperies props = new TrimProperies();
		props.load(new InputStreamReader(new FileInputStream(configPath), "UTF-8"));
		client_id = props.get("client_id");
		client_secret = props.get("client_secret");
		authorization_url = props.get("authorization_url");
		token_url = props.get("token_url");
		issuer_id = props.get("issuer_id");
		redirect_uri = props.get("redirect_uri");
	}

	public String getJWT(String user, String password) throws Exception
	{
		// initialize login process
		String url = issuer_id + "/idaas/mtfim/sps/apiauthsvc?PolicyId=urn:ibm:security:authentication:asf:basicldapuser";
		RestCallResults res = sendGet(url, baseHeaders());
		JSONObject json =  parseError("initialize login: ", res, HttpURLConnection.HTTP_OK);
//		System.out.println(json.write(true));

		// the call generated cookies: JSESSIONID, PD_STATEFUL_<id>, PD-S-SESSION-ID, DPJSESSIONID-DAL
		// pass the cookies to the next stage
		TreeMap<String,String> cookies = new TreeMap<String,String>();
		getCookies(res, cookies);
		Map<String,String> headers = baseHeaders();
		setCookies(headers, cookies);

		// login with user/password
		String location = json.getString("location");
		url = issuer_id + location;
//		System.out.println(url);
		JSONObject body = new JSONObject();
		body.put("username", user);
		body.put("password", password);
		res = sendPut(url, body.write(), headers);
		parseError("login: ", res,  HttpURLConnection.HTTP_NO_CONTENT);

/*
		// update the cookies, print user info
		getCookies(res, cookies);
		headers = baseHeaders();
		setCookies(headers, cookies);
		url = issuer_id + "/v1/mgmt/idaas/user";
		res = sendGet(url, headers);
		json = parseError("user info: ", res, HttpURLConnection.HTTP_OK);
		System.out.println(json.write(true));
*/

		// request authorization code
		getCookies(res, cookies);
		headers = baseHeaders();
		setCookies(headers, cookies);
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		String content = "response_type=code&scope=openid&client_id=" + client_id + "&redirect_uri=" + redirect_uri;
//		headers.put("Content-Length", content.length() + ""); // in bytes?

		//	this redirects to a callback specified in the application, with an authorization code
		// skip the redirection - just extract the authorization code
		res = sendPost(authorization_url, content, headers);
		parseError("request authorization code: ", res, HttpURLConnection.HTTP_MOVED_TEMP); // expected status: redirect
		String redirected = res.headers.get("Location").get(0);
		final String code = "code=";
		int loc = redirected.indexOf(code);
		String authCode = redirected.substring(loc + code.length());
//		System.out.println(redirected);
//		System.out.println(authCode);

		// send request directly to the token generator with the extracted authorization code
		getCookies(res, cookies);
		headers = baseHeaders();
		setCookies(headers, cookies);
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		String authString = client_id + ":" + client_secret;
		String encoded = DatatypeConverter.printBase64Binary(authString.getBytes("UTF-8"));
		headers.put("Authorization", "Basic " + encoded);

		content = "grant_type=authorization_code&code=" + authCode + "&redirect_uri=" + redirect_uri;
		res = sendPost(token_url, content, headers);
		json = parseError("get token: ", res, HttpURLConnection.HTTP_OK);
		String jwt = json.getString("id_token");

//		System.out.println(json.write(true));
//		System.out.println(jwt);
		return jwt;
	}
	public String getSsoUrl(String url, String stage)
	{
		// todo: for more flexibility, put the pemFile name in the airlock properties file or in ssoProperties file
		String parm = stage.equals("PRODUCTION") ? "blueidSSL.pem" : "prepiam.toronto.ca.ibm.com.pem";
		return url + "authentication/sso?key=" + parm;
	}

	Map<String,String> baseHeaders()
	{
		Map<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		headers.put("Cache-Control", "no-cache");
		//headers.put("User-Agent", "curl/7.54.1");
		//headers.put("Host", "prepiam.toronto.ca.ibm.com");
		return headers;
	}
	void getCookies(RestCallResults res, Map<String,String> cookies)
	{
		List<String> list = res.headers.get("Set-Cookie");
		if (list != null)
			for (String cookie : list)
			{
				String[] parts = cookie.split(";");
				if (parts.length > 1)
				{
					String[] items = parts[0].split("=");
					cookies.put(items[0], items[1]);
				}
			}
	}

	void setCookies(Map<String,String> headers, Map<String,String> cookies)
	{
		if (cookies.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String,String> ent : cookies.entrySet())
		{
			if (sb.length() > 0)
				sb.append("; ");
			sb.append(ent.getKey());
			sb.append("=");
			sb.append(ent.getValue());
		}
		headers.put("Cookie", sb.toString());
	}

	public static RestCallResults sendGet(String urlString, Map<String,String> headers) throws Exception {
		
		URL obj = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			con.setRequestProperty(ent.getKey(), ent.getValue());
		}

		return buildResult(con);
	}
	public static RestCallResults sendPut(String urlString, String body, Map<String,String> headers) throws IOException{
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		con.setRequestMethod("PUT");
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			con.setRequestProperty(ent.getKey(), ent.getValue());
		}

		con.setDoOutput(true);
		con.getOutputStream().write(body.getBytes("UTF-8"));
		return buildResult(con);
	}
	public static RestCallResults sendPost(String urlString, String body, Map<String,String> headers) throws IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		con.setRequestMethod("POST");
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			 con.setRequestProperty(ent.getKey(), ent.getValue());
		}

	   con.setInstanceFollowRedirects(false);
	   con.setDoOutput(true);
	   con.getOutputStream().write(body.getBytes("UTF-8"));
	   return buildResult(con);
	}
	public static RestCallResults buildResult(HttpURLConnection con)  throws IOException
	{
		int responseCode = con.getResponseCode();
		Map<String, List<String>> headers = con.getHeaderFields();

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

		return new RestCallResults (out, responseCode, headers);
	}
	JSONObject parseError(String prefix, RestCallResults res, int expected) throws Exception
	{
		if (res.code != expected)
		{
			throw new Exception(String.format("%s expected %d, got %d: %s", prefix, expected, res.code, res.message));
		}

		if (res.message.isEmpty())
			return new JSONObject();

		JSONObject response;
		try {
			response = new JSONObject(res.message);
		}
		catch (Exception e) // not a JSON; serious error
		{
			throw new Exception(res.message);
		}

		String err = response.optString("execptionMsg");
		if (err != null && !err.equals("NA"))
		{
			throw new Exception(prefix + err);
		}
		return response;
	}
}
