package com.ibm.airlock.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.Version;
import com.ibm.airlock.engine.Visit;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.testdriver.ConnectToSso;
import tests.restapi.testdriver.SessionStarter;

// insert ordering rules into a feature tree
public class InsertOrdering
{
	//---------------------------------------------------
	public static class Config
	{
		TrimProperies properties;

		String url, productId, seasonId, stage;
		String branch, branchPath, seasonVersion, sessionToken;
		String oktaUrl, oktaApp, user, password, ssoProperties;

		public Config(String propertiesPath) throws Exception
		{
			System.out.println("properties file: " + propertiesPath);

			properties = new TrimProperies();
			properties.load(new InputStreamReader(new FileInputStream(propertiesPath), "UTF-8"));
			System.out.println(properties.toString());
			System.out.println();

			url = properties.get("url");
			url += url.endsWith("/") ? "admin/" : "/admin";

			productId = properties.get("productId");
			seasonId = properties.get("seasonId");
			sessionToken = properties.get("sessionToken", null);
			stage = properties.get("stage", "DEVELOPMENT");

			branch = properties.get("branch", "MASTER");
			seasonVersion = properties.get("seasonVersion", "2.5");
			Version v = Version.find(seasonVersion);
			branchPath = (v.i < Version.v3_0.i) ? "" : "/branches/" + branch;

			oktaUrl = properties.get("oktaUrl", null);
			oktaApp = properties.get("oktaApp", null);
			user = properties.get("user", null);
			password = properties.get("password", null);
			ssoProperties = properties.get("ssoProperties", null);

			if (oktaUrl != null && oktaApp != null && user != null && password == null)
			{
				SessionStarter st = new SessionStarter(url, oktaUrl, oktaApp);
				sessionToken = st.getJWT(user, password);
				System.out.println("Airlock JWT:" + sessionToken);
			}
			else if (ssoProperties != null && user != null && password != null)
			{
				ConnectToSso sso = new ConnectToSso(ssoProperties);
				String ssoJwt = sso.getJWT(user, password);
				System.out.println("SSO JWT:" + ssoJwt);

				String authUrl = sso.getSsoUrl(url, stage);
				RestClientUtils.RestCallResults results = RestClientUtils.sendGet(authUrl, ssoJwt);
				if (results.code != 200)
					throw new Exception(results.message);

				sessionToken =  results.message; 
				System.out.println("Airlock JWT: " + sessionToken);
			}
		}
	}
	public static class Parms extends Config
	{
		File dir;
		String dataFolder, insertsFile;

		public Parms(String propertiesPath) throws Exception
		{
			super(propertiesPath);
			insertsFile = properties.get("insertsFile");
			dataFolder = properties.get("dataFolder");
			dir = new File(dataFolder);
			if (!dir.isDirectory())
				throw new Exception("missing data folder " + dataFolder);
		}
	}
	//---------------------------------------------------

	Parms conf;
	Map<String,String> name2id = new TreeMap<String,String>();
	Map<String,JSONObject> name2feature = new TreeMap<String,JSONObject>();

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage:  InsertOrdering  InsertOrdering.properties");
			return;
		}

		try {
			new InsertOrdering().run(args[0]);
			System.out.println("done");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(String propertiesPath) throws Exception
	{
		conf = new Parms(propertiesPath);
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(conf.url + "/products/seasons/" + conf.seasonId + conf.branchPath + "/features", conf.sessionToken);
		JSONObject features = parseError(res.message, "features");
		//Utilities.writeString(features.write(true), "c:/features.txt");

		JSONObject root = features.getJSONObject("root");
		new MapName().run(root, null);

		JSONObject json = Utilities.readJson(conf.insertsFile);
		JSONArray items = json.getJSONArray("items");
		for (int i = 0; i < items.length(); ++i)
			insertOne(items.getJSONObject(i));

	}

	void insertOne(JSONObject item) throws Exception
	{
		String parentName = item.getString("parent");
		String ruleName = item.getString("ruleName");
		System.out.println("adding '" + ruleName + "' to '" + parentName + "'");

		String parentId = name2id.get(parentName);
		if (parentId == null)
			throw new Exception("unknown parent name " + parentName);

		String rule = getContent(item, "rule");
		String configuration = getContent(item, "configuration");
		
		// note: InsertPmml does not generate a final configuration { "namespace.name " : calculation_output.result }
		// since the name must be replaced by a UUID known at a later time.
		// Instead it generated two lines which are processed here
		String[] parts = configuration.split("\\n\\s*");
		if (parts.length != 2)
			throw new Exception("unexpected configuration " + configuration);
		String id = name2id.get(parts[0].trim());
		if (id == null)
			throw new Exception("unknown configuration field " + parts[0]);
		String actualConfig = "{'" + id + "' : " + parts[1].trim() +  "}";

		RestClientUtils.RestCallResults res;
		JSONObject parent = name2feature.get(parentName);

		String childId = name2id.get(ruleName);
		boolean update = (childId != null);

		if (update)
		{
			verifyParent(parent, ruleName);
			JSONObject updating = name2feature.get(ruleName);
			updating.put("configuration", actualConfig);
			JSONObject newrule = new JSONObject();
			newrule.put("ruleString", rule);
			updating.put("rule", newrule);

			System.out.println("updating: " + updating.write(true));
			String featureJson = updating.toString();

			res = RestClientUtils.sendPut(conf.url + "/products/seasons" + conf.branchPath + "/features/" + childId, featureJson, conf.sessionToken);
			parseError(res.message, "update feature");
		}
		else
		{
			String template = getContent(item, "template");
			JSONObject adding = new JSONObject(template);

			int pos = ruleName.indexOf('.');
			if (pos < 1)
				throw new Exception("ruleName is not namspace.name " + ruleName);
			String namespace = ruleName.substring(0, pos).trim();
			String name = ruleName.substring(pos+1).trim();
			if (namespace.isEmpty() || name.isEmpty())
				throw new Exception("ruleName is not namspace.name " + ruleName);
	 
			adding.put("namespace", namespace);
			adding.put("name", name);

			adding.put("configuration", actualConfig);
			JSONObject newrule = new JSONObject();
			newrule.put("ruleString", rule);
			adding.put("rule", newrule);

			System.out.println("adding: " + adding.write(true));
			String featureJson = adding.toString();

			res = RestClientUtils.sendPost(conf.url + "/products/seasons/" + conf.seasonId + conf.branchPath + "/features?parent=" + parentId, featureJson, conf.sessionToken);
			parseError(res.message, "update feature");
		}
	}

	String getContent(JSONObject item, String key) throws Exception
	{
		final String filePrefix = "FILE/";

		String content = item.getString(key);

		if (content.startsWith(filePrefix))
		{
			String fileName = content.substring(filePrefix.length());
			File f = new File(conf.dir, fileName);
			if (!f.isFile())
				throw new Exception("unknown " + key + " file " + content);

			content = Utilities.readString(f.getAbsolutePath());
		}
		return content;
	}

	void verifyParent(JSONObject parent, String childName) throws Exception
	{
		JSONArray array = parent.getJSONArray("orderingRules");
		for (int i = 0; i < array.size(); ++i)
		{
			JSONObject child = array.getJSONObject(i);
			String name = getName(child);
			if (name.equals(childName))
				return;
		}
		throw new Exception(childName + " is not under " + getName(parent));
	}

	public static JSONObject parseError(String result, String itemName) throws Exception
	{
		if (result.isEmpty())
			return new JSONObject();

		JSONObject response;
		try {
			response = new JSONObject(result);
		}
		catch (Exception e) // not a JSON; serious error
		{
			String err = (itemName == null) ? "" : "error in " + itemName + ": ";
			throw new Exception(err + result);
		}

		if (response.containsKey("error"))
		{
			String err = (itemName == null) ? "" : "error in " + itemName + ": ";
			throw new Exception(err + response.getString("error"));
		}
		return response;
	}
	BaseAirlockItem.Type getNodeType(JSONObject obj) throws JSONException
	{
		String str = obj.getString(Constants.JSON_FEATURE_FIELD_TYPE);
		return BaseAirlockItem.Type.valueOf(str); // null on error
	}
	String getId(JSONObject obj) throws JSONException
	{
		return obj.getString(Constants.JSON_FIELD_UNIQUE_ID); 
	}
	String getName(JSONObject obj) throws JSONException
	{
		switch (getNodeType(obj))
		{
		case ROOT: return Constants.ROOT_FEATURE;

		case MUTUAL_EXCLUSION_GROUP:
		case CONFIG_MUTUAL_EXCLUSION_GROUP:
		case ORDERING_RULE_MUTUAL_EXCLUSION_GROUP:
			return "mx." + getId(obj);

		case  FEATURE:
		case  CONFIGURATION_RULE:
		case ORDERING_RULE:
		default:
			return obj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + "." + obj.getString(Constants.JSON_FIELD_NAME);
		}
	}
	//------------------------------------------------------
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
		public String get(String key, String defaultValue)
		{
			String out = super.getProperty(key, defaultValue);
			return (out == null) ? null : out.trim();
		}
	}

	class MapName extends Visit
	{
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			String name = getName(obj);
			name2id.put(name, getId(obj));
			name2feature.put(name, obj);
			return null;
		}
	}
}
