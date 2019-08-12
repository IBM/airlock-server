package com.ibm.airlock.admin.translations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


//import org.apache.http.Header;
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
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants.IssueStatus;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.Utilities.Props;
import com.ibm.airlock.admin.serialize.TranslatorLogWriter;

public class SmartlingClient
{
	static final String AUTH_API = "/auth-api/v2/";
	static final String FILE_API = "/files-api/v2/";
	static final String ISSUE_API = "/issues-api/v2/";
	static final String STRING_API = "/strings-api/v2/";
	static final String PROJECT_API = "/projects-api/v2/";
	static final String TRANSLATION_API = "/translations-api/v2/";
	static final String DEFAULT_URL = "https://api.smartling.com/";

	public enum IssueType { SOURCE, TRANSLATION }
	public enum IssueSubType { CLARIFICATION, MISSPELLING, POOR_TRANSLATION, DOES_NOT_FIT_SPACE, PLACEHOLDER_ISSUE, REVIEW_TRANSLATION } // GENERAL_ISSUE not implemented yet

	TranslatorLogWriter logger = null; // for debugging only

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
	public static class RestCallResults
	{
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

			//String xliff = Utilities.readString("C:/client/smartling/test.xliff");
			jj = client.SetTranslation(null, "fr-FR", "C:/client/smartling/test.xliff");
			//jj =  client.getLastModified("basic_test_4.json");
			//client.uploadFle("yd_test2.txt", "C:/client/translate/yd_test2.txt", FileType.JSON, true);
			//client.getAllTranslations("yd_test2.txt", "C:/client/translate/yd_test2.zip", "pending",  true);
			//client.uploadFle("yd_test2.txt", "C:/client/translate/test1.txt", FileType.JSON, true);
			//client.getFileStatus("dummy");
			//jj = client.listFiles();

			//jj = client.getProjectLocales(null);

			//jj = client.createStrings("C:/client/translate/string1.txt");
			//System.out.println(jj.write(true));
			
		//	JSONObject body = Utilities.readJson("C:/client/translate/issue3.json");
		//	String key = client.createIssue(null, body);
		//	client.closeOrReopenIssue(null, "14d1a0a67ae5", true);
		//	client.closeOrReopenIssue(null, "e08a6ad73658", true);
		//	jj = client.getStringIssues(null, "d159a87fc718ed54edf4c744cabc2d49", null);
			System.out.println(jj.write(true));
			//System.out.println("third issue id for same text = " + key);

			//jj = client.getIssue(null, "57f228f25e2b");
			//System.out.println(jj.write(true));

			//Map<String,List<String>> out = client.getSourceIssues(null);
			//System.out.println(out);

		//	jj = client.getIssueComments(null, "bb8589d1591e");
		//	System.out.println(jj.write(true));

		//	client.closeOrReopenIssue(null, "bb8589d1591e", false);
		//	jj = client.getIssue(null, "bb8589d1591e");
		//	System.out.println(jj.write(true));

		//	client.editIssueText(null, "bb8589d1591e", "this is just a test, please ignore (UPDATED)");
		//	client.createIssueComment(null, "bb8589d1591e", "This is comment 2");
			/*
			jj = client.getIssue(null, "bb8589d1591e");
			System.out.println(jj.write(true));
			client.createIssueComment(null, "bb8589d1591e", "This is comment 3");
			jj = client.getIssueComments(null, "bb8589d1591e");
			System.out.println(jj.write(true));
			jj = client.getIssue(null, "bb8589d1591e");
			System.out.println(jj.write(true));
*/
		//	client.editIssueText(null, "bb8589d1591e", "this is just a test, please ignore (UPDATED)");
			
			//ArrayList<String> hashCodes = new ArrayList<String>();
			//hashCodes.add("550d488ee6564831aa15be1129e2269e");
			//hashCodes.add("19527e6cd13f3f7cf4d81957f1d10276");
			//jj = client.getStringTranslations(hashCodes, "fr-FR");

			//jj = client.createStrings("C:/client/translate/string1.txt");
			//jj = client.downloadOriginalFle("basic_test_4.json");

			//System.out.println(jj.write(true));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public SmartlingClient(String propertyFile) throws Exception
	{
		Props properties = new Props();
		properties.load(propertyFile);
		init(properties.get("url"), properties.get("userIdentifier"), properties.get("userSecret"), properties.get("projectId"));
	}
	public SmartlingClient(String user, String userSecret, String project)
	{
		init(DEFAULT_URL, user, userSecret, project);
	}
	void init(String url, String user, String userSecret, String project)
	{
		config = new Config();
		config.url = url;
		config.userIdentifier = user;
		config.userSecret = userSecret;
		config.projectId = project;

		// force initial request to authenticate
		Date now = new Date();
		config.accessExpiration = now;
		config.refreshExpiration = now;
	}
	public void setProject(String project)
	{
		config.projectId = project;
	}
	public void setLogger(TranslatorLogWriter logger)
	{
		this.logger = logger;
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
	void uploadFle(String fileUri, String filePath, SmartlingFileType fileType, boolean authorize) throws Exception
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
		//System.out.println(result.code + ", " +  result.message);
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
		//System.out.println(result.code + ", " +  result.message);
		return parseResult(result);
	}

	// GET - /files-api/v2/projects/{{projectId}}/locales/all/file/zip
	void getAllTranslations(String fileUri, String outputZipPath, String retrievalType,  Boolean includeOriginalStrings) throws Exception
	{
		checkExpiration();
		String parms = "?fileUri=" + encodeURL(fileUri);

		if (retrievalType != null) // to do: validate on of the following: pending, published, pseudo, contextMatchingInstrumented
			parms += "&retrievalType=" + retrievalType;

		if (includeOriginalStrings != null)
			parms += "&includeOriginalStrings=" + includeOriginalStrings;

		String url = config.url + FILE_API + "projects/" + config.projectId + "/locales/all/file/zip" + parms;

		HttpGet get = new HttpGet(url);
		get.addHeader(HttpHeaders.AUTHORIZATION, config.accessToken);

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
	String createIssue(String project, JSONObject body) throws Exception
	{
		checkExpiration();

		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues";
		RestCallResults res = sendPost(url, body.toString(), standardHeaders());
		JSONObject out = parseResult(res);
		return out.getJSONObject("data").getString("issueUid");
	}

	//POST - /issues-api/v2/projects/{projectId}/issues/{issueUid}/issueText
	void editIssueText(String project, String issueUid, String text) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("issueText", text);

		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues/" + issueUid + "/issueText";

		// NOTE: the documentation says to use POST but in fact this is a PUT call
		RestCallResults res = sendPut(url, body.toString(), standardHeaders());
		if (res.code != 200)
			parseResult(res);
	}

	// PUT - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/state
	void closeOrReopenIssue(String project, String issueUid, boolean close) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("issueStateCode", close ? "RESOLVED" : "OPENED");
		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues/" + issueUid + "/state";
		RestCallResults res = sendPut(url, body.toString(), standardHeaders());
		if (res.code != 200)
			parseResult(res);
	}

	//POST - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/comments
	String createIssueComment(String project, String issueUid, String comment) throws Exception
	{
		checkExpiration();
		JSONObject body = new JSONObject();
		body.put("commentText", comment);

		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues/" + issueUid + "/comments";
		RestCallResults res = sendPost(url, body.toString(), standardHeaders());
		JSONObject out = parseResult(res);
		return out.getJSONObject("data").getString("issueCommentUid");
	}

	//GET - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}
	JSONObject getIssue(String project, String issueUid) throws Exception
	{
		checkExpiration();
		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues/" + issueUid;
		//System.out.println(url);
		RestCallResults result = sendGet(url);
		JSONObject obj = parseResult(result);
		return obj.getJSONObject("data");
	}

	//GET - /issues-api/v2/projects/{{projectId}}/issues/{{issueUid}}/comments
	JSONObject getIssueComments(String project, String issueUid) throws Exception
	{
		checkExpiration();
		if (project == null)
			project = config.projectId;
		String url = config.url + ISSUE_API + "projects/" +  project + "/issues/" + issueUid + "/comments";
		RestCallResults result = sendGet(url);
		JSONObject obj = parseResult(result);
		return obj.getJSONObject("data");
	}

	// return all source issues (opened by Smartling) for the project
	//POST - /issues-api/v2/issues-api/v2/projects/projectId/issues/list
	final int chunk = 30;
	Map<String,List<String>> getSourceIssues(String project) throws Exception
	{
		String url = issueListUrl(project);
		JSONObject filter = new JSONObject();
		JSONArray typeCodes = new JSONArray();
		typeCodes.add("SOURCE");
		filter.put("issueTypeCodes", typeCodes);
		filter.put("limit", chunk);

		Map<String,List<String>> out = new TreeMap<String,List<String>>();
		int found = 0;
		for (int offset = 0 ; ; offset += found)
		{
			filter.put("offset", offset);

			checkExpiration();
			RestCallResults res = sendPost(url, filter.toString(), standardHeaders());
			JSONObject obj = parseResult(res);

			JSONObject data = obj.getJSONObject("data");
			JSONArray items = data.optJSONArray("items");
			if (items == null || items.isEmpty())
				break;

			found =  items.size();
			for (int i = 0; i < found; ++i)
			{
				JSONObject item = items.getJSONObject(i);
				String issue = item.getString("issueUid");
				JSONObject string = item.getJSONObject("string");
				String hash = string.getString("hashcode");
				addItem(out, hash, issue);
			}
		}
		return out;
	}
	void addItem(Map<String,List<String>> out, String hash, String issue)
	{
		List<String> list = out.get(hash);
		if (list == null)
		{
			list = new ArrayList<String>();
			out.put(hash, list);
		}
		list.add(issue);
	}

	JSONObject getStringIssues(String project, String hash, String locale) throws Exception
	{
		ArrayList<JSONObject> issues = collectStringIssues(project, hash, locale);
		Collections.sort(issues, new SortByDate());
		//issues.sort(new SortByDate()); only in java 8

		IssueStatus status = IssueStatus.NO_ISSUES;
		for (JSONObject obj : issues)
		{
			String state = obj.getString("issueStateCode");
			if (state.equals("OPENED"))
				status = IssueStatus.HAS_OPEN_ISSUES;
			else if (status == IssueStatus.NO_ISSUES)
				status = IssueStatus.HAS_ISSUES;

			// add the comments
			String issueUid = obj.getString("issueUid");
			JSONObject comments = getIssueComments(project, issueUid);
			JSONArray items = comments.getJSONArray("items");
			obj.put("items", items);
		}

		JSONObject out = new JSONObject();
		out.put("status", status.toString());
		out.put("issues", new JSONArray(issues));
		return out;
	}
	
	String getIssueStatus(String project, String hash, String locale) throws Exception
	{
		ArrayList<JSONObject> issues = collectStringIssues(project, hash, locale);
		IssueStatus status = IssueStatus.NO_ISSUES;
		for (JSONObject obj : issues)
		{
			String state = obj.getString("issueStateCode");
			if (state.equals("OPENED"))
				status = IssueStatus.HAS_OPEN_ISSUES;
			else if (status == IssueStatus.NO_ISSUES)
				status = IssueStatus.HAS_ISSUES;
		}
		return status.toString();
	}
	ArrayList<JSONObject> collectStringIssues(String project, String hash, String locale) throws Exception
	{
		String url = issueListUrl(project);
		if (locale != null)							// use null for English
			locale = SmartlingLocales.get(locale);	// convert to Smartling locale

		JSONObject filter = makeFilter(hash, locale);
		ArrayList<JSONObject> issues = new ArrayList<JSONObject>();

		int found = 0;
		for (int offset = 0 ; ; offset += found)
		{
			filter.put("offset", offset);

			checkExpiration();
			RestCallResults res = sendPost(url, filter.toString(), standardHeaders());
			JSONObject obj = parseResult(res);

			JSONObject data = obj.getJSONObject("data");
			JSONArray items = data.optJSONArray("items");
			if (items == null || items.isEmpty())
				break;

			found = items.size();
			for (int i = 0; i < found; ++i)
			{
				issues.add(items.getJSONObject(i));
			}
		}
		return issues;
	}
	static class SortByDate implements Comparator<JSONObject>
	{
		public int compare(JSONObject o1, JSONObject o2) 
		{
			try {
				String s1 = o1.getString("createdDate");
				String s2 = o2.getString("createdDate");
				return s1.compareTo(s2); // lexical compare of ISO dates
			}
			catch (JSONException e) {
				return 0;
			}
		}
	}
	String issueListUrl(String project)
	{
		if (project == null)
			project = config.projectId;
		return config.url + ISSUE_API + "projects/" +  project + "/issues/list";
	}
	JSONObject makeFilter(String hash, String locale) throws Exception
	{
		JSONObject filter = new JSONObject();
		filter.put("limit", chunk);
		filter.put("issueTypeCodes", makeArray( (locale == null) ? "SOURCE" : "TRANSLATION") );
	
		JSONObject stringFilter = new JSONObject();
		stringFilter.put("hashcodes", makeArray(hash));
		if (locale != null)
			stringFilter.put("localeIds", makeArray(locale));

		filter.put("stringFilter", stringFilter);
		return filter;
	}

	JSONArray makeArray(String value)
	{
		JSONArray out = new JSONArray();
		out.add(value);
		return out;
	}
 
	// Map<Smartlinghash, Map<SmartlingLocale,IssueStatus>>
	@SuppressWarnings("serial")
	public static class ProjectIssues extends HashMap<String,Map<String,IssueStatus>>
	{}

	ProjectIssues getAllIssueStatus(String project) throws Exception
	{
		ProjectIssues out = new ProjectIssues();
		String url = issueListUrl(project);
		JSONObject filter = new JSONObject();
		filter.put("limit", chunk);

		// may not be needed - if omitted, will return both source and translation issues?
		JSONArray typeCodes = new JSONArray();
		typeCodes.add("SOURCE");
		typeCodes.add("TRANSLATION");
		filter.put("issueTypeCodes", typeCodes);

		int found = 0;
		for (int offset = 0 ; ; offset += found)
		{
			filter.put("offset", offset);

			checkExpiration();
			RestCallResults res = sendPost(url, filter.toString(), standardHeaders());
			JSONObject obj = parseResult(res);

			JSONObject data = obj.getJSONObject("data");
			JSONArray items = data.optJSONArray("items");
			if (items == null || items.isEmpty())
				break;

			found = items.size();
			for (int i = 0; i < found; ++i)
			{
				JSONObject item = items.getJSONObject(i);
				String state = item.getString("issueStateCode");
				JSONObject string = item.getJSONObject("string");
				String hash = string.getString("hashcode");
				String localeId = string.optString("localeId", "en-US");
				addIssueState(out, hash, localeId, state);
			}
		}
		return out;
	}
	void addIssueState(Map<String,Map<String,IssueStatus>> out, String hash, String localeId, String state)
	{
		Map<String,IssueStatus> data = out.get(hash);
		if (data == null)
		{
			data = new HashMap<String,IssueStatus>();
			out.put(hash, data);
		}

		IssueStatus oldStatus = data.get(localeId);
		if (oldStatus != IssueStatus.HAS_OPEN_ISSUES) // null always overridden; closed issue overridden by open issue
		{
			IssueStatus status = state.equals("OPENED") ? IssueStatus.HAS_OPEN_ISSUES : IssueStatus.HAS_ISSUES;
			data.put(localeId, status);
		}
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
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/source-strings?" + hashes(hashCodes);
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	//GET - /strings-api/v2/projects/{{projectId}}/translations
	JSONObject getStringTranslations(ArrayList<String> hashCodes, String locale) throws Exception
	{
		checkExpiration();
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/translations?" + hashes(hashCodes) + "&targetLocaleId=" + locale;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	String hashes(ArrayList<String> hashCodes)
	{
		StringBuilder b = new StringBuilder();
		for (String code : hashCodes)
		{
			if (b.length() > 0)
				b.append("&");
			b.append("hashcodes=");
			b.append(code);
		}
		return b.toString();
	}

	JSONObject getOneStringTranslations(String hashCode, String allLocales) throws Exception
	{
		checkExpiration();
		String url = config.url + STRING_API + "projects/" +  config.projectId + "/translations?hashcodes=" + hashCode + "&" + allLocales;
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}

	//--------------------------------------------------------------------------------
	// Project API

	//GET /projects-api/v2/projects/{projectId}
	public JSONObject getProjectLocales(String project) throws Exception
	{
		checkExpiration();
		if (project ==  null)
			project = config.projectId;
		String url = config.url + PROJECT_API + "projects/" +  project; // Optional param: includeDisabledLocales
		RestCallResults result = sendGet(url);
		return parseResult(result);
	}
	public TreeSet<String> getProjectLocaleSet(String project) throws Exception
	{
		JSONObject parsed = getProjectLocales(project);
		TreeSet<String> out = new TreeSet<String>();
		JSONObject data = parsed.getJSONObject("data");
		JSONArray array = data.getJSONArray("targetLocales");
		for (int i = 0; i < array.size(); ++i)
		{
			JSONObject locale = array.getJSONObject(i);
			boolean enabled = locale.getBoolean("enabled");
			String id = locale.getString("localeId");
			if (enabled)
				out.add(id);
		}
		return out;
	}
	//--------------------------------------------------------------------------------
	// Translation API

	//POST /translations-api/v2/projects/<projectUid>/locales/<localeId>/content
	// updates a translation based on the hash provided in the xliff file
	public JSONObject SetTranslation(String project, String localeId, String pathToXliff) throws Exception
	{
		checkExpiration();
		if (project ==  null)
			project = config.projectId;
		String url = config.url + TRANSLATION_API + "projects/" +  project + "/locales/" + localeId + "/content";

		Map<String,String> headers = new TreeMap<String,String>();
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);

		RestCallResults res = multiPartPost(url, pathToXliff, headers);
		return parseResult(res);
	}
	//--------------------------------------------------------------------------------

	Map<String,String> standardHeaders()
	{
		Map<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");
		headers.put(HttpHeaders.AUTHORIZATION, config.accessToken);
		return headers;
	}

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

	// synchronized, because ISSUE API and PROJECT API may be used directly by the server and not just from the background thread
	synchronized void checkExpiration() throws Exception
	{
		Date now = new Date();

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

	RestCallResults multiPartPost(String url, String filePath, Map<String, String> headers) throws Exception
	{
		if (logger != null)
			logger.fine("multiPartPost " + url);

//		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpPost uploadFile = new HttpPost(url);
		uploadFile.addHeader(HttpHeaders.AUTHORIZATION, config.accessToken);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		for (Map.Entry<String,String> e : headers.entrySet())
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
			/*if (responseCode != 200 && responseCode != 202)
			{
				System.out.println("code " + responseCode);
				for (Header h : response.getAllHeaders())
					System.out.println(h.toString());
			}*/
		}

		return new RestCallResults (out, responseCode);
	}
	RestCallResults sendGet(String url) throws Exception
	{
		if (logger != null)
			logger.fine("sendGet " + url);

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
	RestCallResults sendPut(String url, String parameters, Map<String,String> headers) throws Exception
	{
		if (logger != null)
			logger.fine("sendPut " + url);

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("PUT");
		for (Map.Entry<String,String> ent : headers.entrySet()) {
			 con.setRequestProperty(ent.getKey(), ent.getValue());
		}

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

	public RestCallResults sendPost(String urlString, String parameters, Map<String,String> headers) throws IOException
	{
		if (logger != null)
			logger.fine("sendPost " + urlString);

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
