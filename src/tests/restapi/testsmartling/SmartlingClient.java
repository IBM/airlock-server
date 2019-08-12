package tests.restapi.testsmartling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.Utilities.Props;

public class SmartlingClient
{
	static final String AUTH_API = "/auth-api/v2/";
	static final String FILE_API = "/files-api/v2/";
	static final String ISSUE_API = "/issues-api/v2/";
	static final String STRING_API = "/strings-api/v2/";
	static final String PROJECT_API = "/projects-api/v2/";

	public enum issueTypeCode { SOURCE, TRANSLATION }
	public enum issueSubType { CLARIFICATION, MISSPELLING, POOR_TRANSLATION, DOES_NOT_FIT_SPACE, PLACEHOLDER_ISSUE }
	
	static class Config {
		String url;
		String userIdentifier;
		String userSecret;
		String projectId;
		String accessToken;
		String refreshToken;
		Date accessExpiration;
		Date refreshExpiration;
	}
	Config config;

	//----------------------------------------------------------------------------
	public static class RestCallResults {
		public String message;
		public int code;
		
		public RestCallResults(String msg, int code) {
			this.message = msg;
			this.code = code;
		}
	} 
	//----------------------------------------------------------------------------
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: SmartlingClient propertyFile");
			return;
		}

		try {
			JSONObject jj;
			SmartlingClient client = new SmartlingClient(args[0]);
			client.checkExpiration();
			//jj =  client.getLastModified("basic_test_4.json");
			//client.uploadFle("yd_test2.txt", "C:/client/translate/yd_test2.txt", FileType.JSON, true);
			//client.getAllTranslations("yd_test2.txt", "C:/client/translate/yd_test2.zip", "pending",  true);

			//client.uploadFle("yd_test2.txt", "C:/client/translate/test1.txt", FileType.JSON, true);
			//client.getFileStatus("dummy");
			//jj = client.listFiles();
			jj = client.getProjectLocales();
			//jj = client.createStrings("C:/client/translate/string1.txt");
			ArrayList<String> hashCodes = new ArrayList<String>();
			hashCodes.add("550d488ee6564831aa15be1129e2269e");
			hashCodes.add("19527e6cd13f3f7cf4d81957f1d10276");
			//jj = client.getStringTranslations(hashCodes, "fr-FR");
			//jj = client.createStrings("C:/client/translate/string1.txt");
			//jj = client.downloadOriginalFle("basic_test_4.json");

			System.out.println(jj.write(true));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	SmartlingClient(String propertyFile) throws Exception
	{
		config = new Config();
		Props properties = new Props();
		properties.load(propertyFile);

		config.url = properties.get("url");
		config.userIdentifier = properties.get("userIdentifier");
		config.userSecret = properties.get("userSecret");
		config.projectId = properties.get("projectId");

		// force initial request to authenticate
		Date now = new Date(System.currentTimeMillis());
		config.accessExpiration = now;
		config.refreshExpiration = now;
	}

	//--------------------------------------------------------------------------------
	// Authorization API
	//POST - /auth-api/v2/authenticate
	void authenticate() throws Exception
	{
		JSONObject credentials = new JSONObject();
		credentials.put("userIdentifier", config.userIdentifier);
		credentials.put("userSecret", config.userSecret);
		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");

		RestCallResults res = sendPost(config.url + AUTH_API + "authenticate", credentials.toString(), headers);
		JSONObject out = parseResult(res);
		JSONObject data = out.getJSONObject("data");
		updateExpiration(data);
	}

	//POST - /auth-api/v2/authenticate/refresh
	void refresh() throws Exception
	{
		JSONObject body = new JSONObject();
		body.put("refreshToken", config.refreshToken);
		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");

		RestCallResults res = sendPost(config.url + AUTH_API + "authenticate/refresh", body.toString(), headers);
		JSONObject out = parseResult(res);
		JSONObject data = out.getJSONObject("data");
		updateExpiration(data);
	}
	//--------------------------------------------------------------------------------
	// file API
	//POST - /files-api/v2/projects/{{projectId}}/file
	void uploadFle(String fileUri, String filePath, FileType fileType, boolean authorize) throws Exception
	{
		checkExpiration();
		String url = config.url + FILE_API + "projects/" + config.projectId + "/file";

		TreeMap<String,String> parms = new TreeMap<String,String>();
		parms.put("fileUri", fileUri);
		parms.put("fileType", fileType.getIdentifier());
		parms.put("authorize", authorize ? "true" : "false");
		//parms.put("callbackUrl", ...);
		//parms.put("localeIdsToAuthorize", ...);

		RestCallResults result = multiPartPost(url, filePath, parms);
		if (result.code == 302) // redirect
		{
			System.out.println("redirected to " + result.message);
			result = multiPartPost(result.message, filePath, parms);
		}
		System.out.println(result.code + ", " +  result.message);
		parseResult(result);
	}

	// GET - /files-api/v2/projects/{projectId}/file
	JSONObject downloadOriginalFle(String fileUri) throws Exception
	{
		checkExpiration();
		String url = config.url + FILE_API + "projects/" + config.projectId + "/file?fileUri=" + fileUri;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	// GET - /files-api/v2/projects/{{projectId}}/file/status
	JSONObject getFileStatus(String fileUri) throws Exception
	{
		checkExpiration();
		String url = config.url + FILE_API + "projects/" + config.projectId + "/file/status?fileUri=" + encodeURL(fileUri);
		RestCallResults result = sendGet(url);
		//if (result.code == 302) httpGet redirects by default
		//	result = sendGet(result.message);
		System.out.println(result.code + ", " +  result.message);
		return parseResult(result);
	}

	// GET - /files-api/v2/projects/{{projectId}}/locales/all/file/zip
	void getAllTranslations(String fileUri, String outputZipPath, String retrievalType,  Boolean includeOriginalStrings) throws Exception
	{
		checkExpiration();
		String parms = "?fileUri=" + encodeURL(fileUri);

		if (retrievalType != null) // TODO validate on of the following: pending, published, pseudo, contextMatchingInstrumented
			parms += "&retrievalType=" + retrievalType;

		if (includeOriginalStrings != null)
			parms += "&includeOriginalStrings=" + includeOriginalStrings;

		String url = config.url + FILE_API + "projects/" + config.projectId + "/locales/all/file/zip" + parms;

		HttpGet get = new HttpGet(url);
		get.addHeader(HttpHeaders.AUTHORIZATION, config.accessToken); // IS THIS THE RIGHT HEADER?

		CloseableHttpClient client = HttpClientBuilder.create().build();
		CloseableHttpResponse response = client.execute(get);
		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode != 200)
			throw new Exception("Invalid respomnse code from getAllTranslations: " + responseCode);

		HttpEntity entity = response.getEntity();
		InputStream input = entity.getContent();
		FileOutputStream output = new FileOutputStream(outputZipPath);

		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1)
		{
			output.write(buffer, 0, bytesRead);
		}
		output.close();
		client.close();
		response.close();
	}

	// GET - /files-api/v2/projects/{{projectId}}/files/list
	JSONObject listFiles() throws Exception
	{
		checkExpiration();
		String url = config.url + FILE_API + "projects/" + config.projectId + "/files/list";
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	// GET - /files-api/v2/projects/{{projectId}}/file/last-modified 
	JSONObject getLastModified(String fileUri) throws Exception
	{
		checkExpiration();
		String url = config.url + FILE_API + "projects/" + config.projectId + "/file/last-modified?fileUri=" + encodeURL(fileUri);
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	//--------------------------------------------------------------------------------
	// ISSUE API
	//POST - /issues-api/v2/projects/{{projectId}}/issues
	String createIssue(String issueText, issueTypeCode type, issueSubType subtype, String stringHash, String locale) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("issueText", issueText);
		body.put("issueTypeCode", type.toString());
		body.put("issueSubTypeCode", subtype.toString());

		JSONObject string = new JSONObject();
		string.put("hashcode", stringHash);
		string.put("localeId", locale); // e.g "ru-RU"
		body.put("string", string);

		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues";
		RestCallResults res = sendPost(url, body.toString(), headers);
		JSONObject out = parseResult(res);
		return out.getJSONObject("data").getString("issueUid");
	}

	//POST - /issues-api/v1/projects/{projectId}/issues/{issueUid}/issueText
	void editIssueText(String issueUid, String text) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("issueText", text);

		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues/" + issueUid + "/issueText";
		RestCallResults res = sendPost(url, body.toString(), headers);
		if (res.code != 200)
			parseResult(res);
	}

	// PUT - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/state
	void closeOrReopenIssue(String issueUid, boolean close) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("issueStateCode", close ? "RESOLVED" : "OPENED");
		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues/" + issueUid + "/state";
		RestCallResults res = sendPut(url, body.toString());
		if (res.code != 200)
			parseResult(res);
	}

	//POST - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/comments
	String createIssueComment(String issueUid, String comment) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("commentText", comment);

		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues/" + issueUid + "/comments";
		RestCallResults res = sendPost(url, body.toString(), headers);
		JSONObject out = parseResult(res);
		return out.getJSONObject("data").getString("issueCommentUid");
	}

	//GET - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}
	JSONObject getIssue(String issueUid) throws Exception
	{
		checkExpiration();
		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues/" + issueUid;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	//GET - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/comments
	JSONObject getIssueComments(String issueUid) throws Exception
	{
		checkExpiration();
		String url = config.url + ISSUE_API + "projects/" +  config.projectId + "/issues/" + issueUid + "/comments";
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	//--------------------------------------------------------------------------------
	// String API

	// POST - /strings-api/v2/projects/{{projectId}}
	JSONObject createStrings(String StringsFile) throws Exception
	{
		checkExpiration();

		String stringsJson = Utilities.readString(StringsFile);
		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		RestCallResults res = sendPost(config.url + STRING_API + "projects/" +  config.projectId, stringsJson, headers);
		return parseResult(res);
	}
	RestCallResults createStrings(JSONObject json) throws Exception
	{
		checkExpiration();

		TreeMap<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		return sendPost(config.url + STRING_API + "projects/" +  config.projectId, json.toString(), headers);
	}
	//GET - /strings-api/v2/projects/{{projectId}}/processes/{{processUid}}
	JSONObject checkCreateStringsStatus(String processId) throws Exception
	{
		checkExpiration();
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/processes/" + processId;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	//GET - /strings-api/v2/projects/{{projectId}}/source-strings
	JSONObject getSourceStrings(ArrayList<String> hashCodes) throws Exception
	{
		checkExpiration();

		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String code : hashCodes)
		{
			b.append(first ? "?" : "&");
			b.append("hashcodes=");
			b.append(code);
			first = false;
		}
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/source-strings" + b.toString();
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	//GET - /strings-api/v2/projects/{{projectId}}/translations
	JSONObject getStringTranslations(ArrayList<String> hashCodes, String locale) throws Exception
	{
		checkExpiration();

		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String code : hashCodes)
		{
			b.append(first ? "?" : "&");
			b.append("hashcodes=");
			b.append(code);
			first = false;
		}
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/translations" + b.toString() + "&targetLocaleId=" + locale;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	JSONObject getOneStringTranslations(String hashCode, String allLocales) throws Exception
	{
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/translations?hashcodes=" + hashCode + "&" + allLocales;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	//--------------------------------------------------------------------------------
	// Project API

	//GET /projects-api/v2/projects/{projectId}
	JSONObject getProjectLocales() throws Exception
	{
		checkExpiration();
		String url = config.url + PROJECT_API + "projects/" +  config.projectId; // Optional param: includeDisabledLocales
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	//--------------------------------------------------------------------------------

	public JSONObject parseResult(RestCallResults result) throws Exception
	{
		if (result.message.isEmpty())
			return new JSONObject();

		JSONObject response;
		try {
			response = new JSONObject(result.message);
		}
		catch (Exception e) // not a JSON; serious error
		{
			throw new Exception("code " + result.code + ": " + result.message);
		}

		if (response.containsKey("response"))
			response = response.getJSONObject("response");

		if (response.containsKey("errors"))
		{
			throw new Exception("code " + result.code + ": " + response.getString("errors"));
		}

		return response;
	}

	void updateExpiration(JSONObject data) throws JSONException
	{
		int expiresIn = data.getInt("expiresIn");
		int refreshExpiresIn = data.getInt("refreshExpiresIn");

		config.accessToken = "Bearer " + data.getString("accessToken");
		config.refreshToken = data.getString("refreshToken");

		long nowMillis = System.currentTimeMillis();
		config.accessExpiration = new Date(nowMillis + expiresIn*1000);
		config.refreshExpiration = new Date(nowMillis + refreshExpiresIn*1000);
	}

	void checkExpiration() throws Exception
	{
		Date now = new Date(System.currentTimeMillis());

		long diffSeconds = (config.refreshExpiration.getTime() - now.getTime()) / 1000;
		if (diffSeconds < 60)
		{
			 authenticate();
			 return;
		}

		diffSeconds = (config.accessExpiration.getTime() - now.getTime()) / 1000;
		if (diffSeconds < 60)
			refresh();
	}

	RestCallResults multiPartPost(String url, String filePath, TreeMap<String,String> parms) throws Exception
	{
//		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpPost uploadFile = new HttpPost(url);
		uploadFile.addHeader(HttpHeaders.AUTHORIZATION, config.accessToken);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		for (Map.Entry<String,String> e : parms.entrySet())
		{
			builder.addTextBody(e.getKey(), e.getValue(), ContentType.TEXT_PLAIN);
		}

		// attach the file to the POST
		File f = new File(filePath);
		builder.addBinaryBody("file", new FileInputStream(f), ContentType.APPLICATION_OCTET_STREAM, f.getName() );

		HttpEntity multipart = builder.build();
		uploadFile.setEntity(multipart);
		CloseableHttpResponse response = httpClient.execute(uploadFile);

		RestCallResults out = parseResponse(response);
		httpClient.close();
		response.close();

		return out;
	}

	RestCallResults parseResponse(CloseableHttpResponse response) throws Exception
	{
		int responseCode = response.getStatusLine().getStatusCode();
		String out;
		if (responseCode == 302)
		{
			out = response.getFirstHeader("Location").getValue();
		}
		else
		{
			HttpEntity entity = response.getEntity();
			InputStream inp = entity.getContent();
			out = Utilities.streamToString(inp);
			if (responseCode != 200)
			{
				System.out.println("code " + responseCode);
				for (Header h : response.getAllHeaders())
					System.out.println(h.toString());
			}
		}

		return new RestCallResults (out, responseCode);
	}
	RestCallResults sendGet(String url) throws Exception
	{
		System.out.println(url);

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");
		con.setRequestProperty (HttpHeaders.AUTHORIZATION, config.accessToken);
		return buildResult(con);
/*
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url);
		get.addHeader(HttpHeaders.AUTHORIZATION, config.accessToken);

		CloseableHttpResponse response = httpClient.execute(get);
		RestCallResults out = parseResponse(response);
		httpClient.close();
		response.close();
		return out;
*/
	}
	RestCallResults sendPut(String url, String parameters) throws Exception
	{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("PUT");
		con.setRequestProperty (HttpHeaders.AUTHORIZATION, config.accessToken);
		con.setRequestProperty ("Content-Type", "application/json");

		con.setDoOutput(true);
		con.getOutputStream().write(parameters.getBytes("UTF-8"));
		return buildResult(con);
	}
	static String encodeURL(String input)
	{
		StringBuilder resultStr = new StringBuilder();
		for (char ch : input.toCharArray()) {
			if (isUnsafe(ch)) {
				resultStr.append('%');
				resultStr.append(toHex(ch / 16));
				resultStr.append(toHex(ch % 16));
			} else {
				resultStr.append(ch);
			}
		}
		return resultStr.toString();
	}

	static char toHex(int ch) {
		return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
	}

	private static boolean isUnsafe(char ch) {
		if (ch > 128 || ch < 0)
			return true;
		return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
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
	
	public static RestCallResults buildResult(HttpURLConnection con)  throws IOException
	{
		int responseCode = con.getResponseCode();

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
}

