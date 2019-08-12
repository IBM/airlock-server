package com.ibm.airlock.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.analytics.Experiment;
import com.ibm.airlock.admin.analytics.Variant;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.notifications.AirlockNotification;
import com.ibm.airlock.admin.serialize.AirlockFilesWriter;
import com.ibm.airlock.admin.serialize.AuditLogWriter;
import com.ibm.airlock.admin.serialize.DataSerializer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.ActionType;
import com.ibm.airlock.Constants.AttributeType;
import com.ibm.airlock.Constants.BranchStatus;
import com.ibm.airlock.Constants.BranchesOutputMode;
import com.ibm.airlock.Constants.CancelCheckoutMode;
import com.ibm.airlock.Constants.AirlockCapability;
import com.ibm.airlock.Constants.GetAnalyticsOutputMode;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.InputFormat;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.Constants.Platform;
import com.ibm.airlock.Constants.REQUEST_ITEM_TYPE;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Constants.ServiceState;
import com.ibm.airlock.Constants.SimulationType;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.StringStatus;
import com.ibm.airlock.Constants.StringsOutputMode;
import com.ibm.airlock.Constants.TranslationStatus;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.admin.MergeBranch.MergeException;
import com.ibm.airlock.admin.analytics.AirlockAnalytics;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection.FeatureAttributesPair;
import com.ibm.airlock.admin.analytics.AnalyticsDataCollection.FeatureAttributesPair.AttributeTypePair;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;

import com.ibm.airlock.admin.operations.AirlockCapabilities;
import com.ibm.airlock.admin.operations.AirlockChangeContent;
import com.ibm.airlock.admin.operations.UserRoleSets;
import com.ibm.airlock.admin.operations.Roles;
import com.ibm.airlock.admin.operations.UserRoleSets.UserRoleSet;
import com.ibm.airlock.admin.operations.Webhooks;
import com.ibm.airlock.admin.purchases.EntitlementItem;
import com.ibm.airlock.admin.streams.AirlockStream;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.admin.translations.TranslationUtilities;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;

import static com.ibm.airlock.AirLockContextListener.getEnv;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

public class Utilities
{
	public static final Logger logger = Logger.getLogger(Utilities.class.getName());
	public static final String separator = " - ";

	public static class RestCallResults {
		public String message;
		public int code;

		public RestCallResults(String msg, int code) {
			this.message = msg;
			this.code = code;
		}
	}

	public static RestCallResults sendPost(String urlString, String parameters) throws IOException {

		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestMethod("POST");
		con.setConnectTimeout(100);

		con.setDoOutput(true);
		//        JSONObject obj = new JSONObject();
		//        try {
		//			obj.append("helllo", "json");
		//		} catch (JSONException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			return null;
		//		}
		//        try {
		//			obj.write(con.getOutputStream());
		//		} catch (JSONException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			return null;
		//		}

		con.getOutputStream().write(parameters.getBytes("UTF-8"));
		return buildResult(con);
	}
	public static RestCallResults sendPost(String urlString, JSONObject parameters) throws IOException {

		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestMethod("POST");
		con.setConnectTimeout(100);

		con.setDoOutput(true);
		try {
			parameters.write(con.getOutputStream());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		//        con.getOutputStream().write(parameters.getBytes("UTF-8"));
		return buildResult(con);
	}

	private static RestCallResults buildResult(HttpURLConnection con)  throws IOException
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
			out = Utilities.streamToString(inp);

		return new RestCallResults (out, responseCode);
	}

	//items type is Constants.FEATURES_DB_PARAM_NAME or Constants.PURCHASES_DB_PARAM_NAME
	public static String initFromSeasonFeaturesJSON(JSONObject featuresJson, Map<String, Season> seasonsDB, Map<String, BaseAirlockItem> featursDB, DataSerializer ds, boolean updateSeason, Constants.REQUEST_ITEM_TYPE itemsType) throws JSONException {

		String pathSeparator = ds.getSeparator();

		String seasonId = featuresJson.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new JSONException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//calculate season version
			String serverInfoFile = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+pathSeparator+seasonId+pathSeparator+Constants.AIRLOCK_SERVER_INFO_FILE_NAME;
			String seasonVersion = "0"; 
			if (ds.isFileExists(serverInfoFile)) {
				//season.setOldRuntimeFileExists(true);
				JSONObject serverInfoObj = ds.readDataToJSON(serverInfoFile);
				seasonVersion = serverInfoObj.getString(Constants.JSON_FIELD_SERVER_VERSION);
			} 


			JSONObject rootObj = featuresJson.getJSONObject(Constants.JSON_FIELD_ROOT);
			RootItem rootFeature = new RootItem();			

			Environment env = new Environment();
			env.setServerVersion(seasonVersion); 
			env.setServiceState(ServiceState.INITIALIZING);

			rootFeature.fromJSON(rootObj, featursDB, null, env);
			featursDB.put(rootFeature.getUniqueId ().toString(), rootFeature);

			if (updateSeason) {
				if (itemsType.equals(Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS)) {
					season.setEntitlementsRoot(rootFeature);
				}
				else {
					season.setRoot(rootFeature);

					String oldRuntimeFile = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+pathSeparator+seasonId+pathSeparator+Constants.AIRLOCK_RUNTIME_FILE_NAME;

					//if AirlockRuntime.json file exist which means it is an old season.		
					if (ds.isFileExists(oldRuntimeFile)) {
						season.setOldRuntimeFileExists(true);							
					}

					season.setServerVersion(seasonVersion);	
				}
			}
			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
		catch (Exception e) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + e.getMessage());
		}
	}		

	public static void initFromProductsJSON(JSONObject productsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		doInitProductsListFromJson(productsJSON, productsDB, seasonsDB, context);
		/*JSONArray products = productsJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);


		for (int i=0; i<products.size(); i++) {
			JSONObject productJsonObj = products.getJSONObject(i);			
			Product product = new Product();
			product.fromJSON(productJsonObj, seasonsDB, context);

			productsDB.put(product.getUniqueId().toString(), product);
		}	*/		
	}

	public static void doInitProductsListFromJson(JSONObject productsJSON, Map<String, Product> productsDB,	Map<String, Season> seasonsDB, ServletContext context) throws JSONException {
		JSONArray products = productsJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);


		for (int i=0; i<products.size(); i++) {
			JSONObject productJsonObj = products.getJSONObject(i);			
			Product product = new Product();
			product.fromJSON(productJsonObj, seasonsDB, context);

			if (productsDB!=null)
				productsDB.put(product.getUniqueId().toString(), product);
		}	

	}

	public static void initProductFollowersJSON(JSONObject allFollowersJson, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersDB = (Map<String,ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		JSONArray followers = (JSONArray)allFollowersJson.get(Constants.JSON_FIELD_FOLLOWERS);
		for (int i=0; i<followers.size(); i++) {
			JSONObject followersJson = (JSONObject) followers.get(i);
			JSONArray productFollowersArray = (JSONArray) followersJson.get("followers");

			ArrayList<String> productFollowers=new ArrayList<>();
			for(int j=0; j<productFollowersArray.length(); j++) {
				productFollowers.add(productFollowersArray.optString(j));
			}
			followersDB.put(followersJson.get("uniqueID").toString(), productFollowers);
		}
	}

	public static void initFeatureFollowersJSON(JSONObject allFollowersJson, ServletContext context,Map<String,Season> seasonDB) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersDB = (Map<String,ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_FEATURES_DB_PARAM_NAME);
		String seasonId = allFollowersJson.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
		Season season = seasonDB.get(seasonId);
		JSONArray followers = (JSONArray)allFollowersJson.get(Constants.JSON_FIELD_FOLLOWERS);
		for (int i=0; i<followers.size(); i++) {
			JSONObject followersJson = (JSONObject) followers.get(i);
			JSONArray featureFollowersArray = (JSONArray) followersJson.get("followers");

			ArrayList<String> productFollowers=new ArrayList<>();
			for(int j=0; j<featureFollowersArray.length(); j++) {
				productFollowers.add(featureFollowersArray.optString(j));
			}
			String featureId = followersJson.get("uniqueID").toString();
			if(season != null){
				season.addFollowersIds(featureId);
			}
			followersDB.put(featureId, productFollowers);
		}
	}

	public static String validateLegalUUID(String id) {
		try {
			UUID.fromString(id);
		} catch (IllegalArgumentException iae) {
			return iae.getMessage();
		}
		return null;
	}
	/*
	public static InternalUserGroups initFromUserGroupsJSON(JSONObject usrGroupsJSON) throws JSONException {
		InternalUserGroups groupsList = new InternalUserGroups();		
		groupsList.fromJSON(usrGroupsJSON);
		return groupsList;
	}
	 */	
	public static void initFromCapabilitiesJSON(JSONObject capabilitiesJSON, ServletContext context) throws JSONException {
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);		
		capabilities.fromJSON(capabilitiesJSON);
	}

	//if valid return null, else return the error in output String
	public static String validateName (String name) {
		if (name == null || name.isEmpty())
			return "The name field cannot be null or empty.";

		if (!Utilities.isEnglishLetter(name.charAt(0))) {
			return "The name field must start with a letter.";
		}

		for (int i=1; i<name.length(); i++) {
			Character c = name.charAt(i);
			if (!Utilities.isEnglishLetter(c) && !Character.isDigit(c) && !c.equals(' ') && !c.equals('.')) {
				return "The name field can contain English letters, digits, spaces, and periods only.";						
			}
		}

		return null;
	}

	public static boolean isEnglishLetter (Character ch) {
		return ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z'));
	}

	public static String[] jsonArrToStringArr(JSONArray jsonArr) throws JSONException {
		if (jsonArr == null)
			return null;

		String[] res = new String[jsonArr.size()];
		for (int i=0; i<jsonArr.size(); i++) {
			res[i] = jsonArr.getString(i);
		}

		return res;
	}

	public static LinkedList<String> jsonArrToStringsList(JSONArray jsonArr) throws JSONException {
		if (jsonArr == null)
			return null;

		LinkedList<String> res = new LinkedList<String>();
		for (int i=0; i<jsonArr.size(); i++) {
			res.add(jsonArr.getString(i));
		}

		return res;
	}

	public static String StringsListToString(List<String> strList) throws JSONException {
		if (strList == null)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i=0; i<strList.size(); i++) {
			if (i!=0)
				sb.append(",");
			sb.append(strList.get(i));					
		}
		sb.append(']');
		return sb.toString();
	}

	public static String RoleTypeListToString(List<RoleType> strList) throws JSONException {
		if (strList == null)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i=0; i<strList.size(); i++) {
			if (i!=0)
				sb.append(",");
			sb.append(strList.get(i).toString());					
		}
		sb.append(']');
		return sb.toString();
	}

	public static void initFromRolesJSON(JSONObject rolesJSON, ServletContext context) throws JSONException {
		Roles roles = (Roles)context.getAttribute(Constants.ROLES_PARAM_NAME);		
		roles.fromJSON(rolesJSON);
	}

	public static void initFromWebhooksJSON(JSONObject webhooksJSON, ServletContext context) throws JSONException {
		Webhooks webhooks = (Webhooks)context.getAttribute(Constants.WEBHOOKS_PARAM_NAME);		
		webhooks.fromJSON(webhooksJSON);	
	}

	public static void initFromAirlockServersJSON(JSONObject alServersJSON, ServletContext context) throws JSONException {
		AirlockServers alServers = (AirlockServers)context.getAttribute(Constants.AIRLOCK_SERVERS_PARAM_NAME);		
		alServers.fromJSON(alServersJSON);
	}

	public static void initFromAirlockUsersJSON(JSONObject airlockUsersJSON, ServletContext context, Product prod) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);

		if (prod == null) { //init global airlock users
			UserRoleSets airlocUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);		
			airlocUsers.fromJSON(airlockUsersJSON, usersDB);
		}
		else {
			//init per product airlockUsers
			UserRoleSets airlocUsers = prod.getProductUsers();		
			airlocUsers.fromJSON(airlockUsersJSON, usersDB);
		}
	}

	public static InternalUserGroups initFromUserGroupsJson(JSONObject userGroupsJSON, ServletContext context, String productId) throws JSONException {		
		//init per product airlockUsers
		Map<String,InternalUserGroups> perProductUsers = (Map<String, InternalUserGroups>)context.getAttribute(Constants.USER_GROUPS_PER_PRODUCT_PARAM_NAME); 
		InternalUserGroups userGroups = new InternalUserGroups();		
		userGroups.fromJSON(userGroupsJSON);
		perProductUsers.put(productId, userGroups);
		return userGroups;
	}

	public static String errorMsgToErrorJSON(String err) {
		JSONObject errObj = new JSONObject();
		try {
			errObj.put("error", err);
		} catch (JSONException e) {
			//never gets here
		}
		return errObj.toString();
	}

	public static Platform strToPlatform(String platformStr) {
		if (platformStr == null) 
			return null;


		if(platformStr.equalsIgnoreCase(Platform.Android.toString())) 
			return Platform.Android;

		if(platformStr.equalsIgnoreCase(Platform.iOS.toString())) 
			return Platform.iOS;		

		if(platformStr.equalsIgnoreCase(Platform.c_sharp.toString())) 
			return Platform.c_sharp;		

		return null;
	}

	static public LinkedList<String> stringJsonArrayToStringsList(JSONArray stringsArray) throws JSONException {
		if (stringsArray==null)
			return null;

		LinkedList<String> res = new LinkedList<String>();

		for (int i=0; i<stringsArray.size(); i++) {
			res.add(stringsArray.getString(i));
		}

		return res;
	}

	public static boolean jsonObjsAreEqual (JSONObject js1, JSONObject js2) throws JSONException {
		if (js1 == null || js2 == null) {
			return (js1 == js2);
		}

		if (JSONObject.getNames(js1)== null && JSONObject.getNames(js2) == null) {
			//both empty json objects
			return true;
		}

		if (JSONObject.getNames(js1) == null || JSONObject.getNames(js2) == null) {
			//one is empty json and the other not
			return false;
		}

		List<String> l1 =  Arrays.asList(JSONObject.getNames(js1));
		Collections.sort(l1);
		List<String> l2 =  Arrays.asList(JSONObject.getNames(js2));
		Collections.sort(l2);
		if (!l1.equals(l2)) {
			return false;
		}
		for (String key : l1) {
			Object val1 = js1.get(key);
			Object val2 = js2.get(key);
			if (val1 instanceof JSONObject) {
				if (!(val2 instanceof JSONObject)) {
					return false;
				}
				if (!jsonObjsAreEqual((JSONObject)val1, (JSONObject)val2)) {
					return false;
				}
			}

			if (val1 == null) {
				if (val2 != null) {
					return false;
				}
			}  else if (!val1.equals(val2)) {
				return false;
			}
		}
		return true;
	}


	public static String initFromSeasonInputschemaJSON(JSONObject inputSchemaJSON, JSONObject streamsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		String seasonId = inputSchemaJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			//JSONObject schemaJSON = (JSONObject) inputSchemaJSON.get(Constants.JSON_FIELD_INPUT_SCHEMA);
			season.getInputSchema().fromJSON(inputSchemaJSON);
			if (streamsJSON != null)
				season.getInputSchema().mergeSchema(streamsJSON, false, null);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + jsne.getMessage());
		} catch (GenerationException e) {
			throw new JSONException("Failed merging streams in season '" + seasonId +  "': " + e.getMessage());
		}
	}

	public static String initFromEncryptionKeyJSON(JSONObject encryptionKeyJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		String seasonId = encryptionKeyJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);
		try {


			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			String encryptionKey = encryptionKeyJSON.getString(Constants.JSON_FIELD_ENCRYPTION_KEY);

			season.setEncryptionKey(encryptionKey);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing encryption key for season '" + seasonId +  "' JSON: " + jsne.getMessage());
		} 
	}

	public static String initFromSeasonAnalyticsJSON(JSONObject analyticsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		String seasonId = analyticsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			Environment env = new Environment();
			env.setBranchId(Constants.MASTER_BRANCH_NAME);
			env.setServerVersion(season.getServerVersion());

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			//for now no validation is done - error will cause JSONError.			
			season.getAnalytics().fromJSON(season.getUniqueId(), analyticsJSON, context, env, airlockItemsDB);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	public static String initFromSeasonUtilitiesJSON(JSONObject utilitiesJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

		String seasonId = utilitiesJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			JSONArray utilitiesSArr = utilitiesJSON.getJSONArray(Constants.JSON_FIELD_UTILITIES);			
			season.getUtilities().fromJSON(utilitiesSArr, utilitiesDB);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season utilities '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	public static String initFromSeasonStreamsJSON(JSONObject streamsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);

		String seasonId = streamsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			JSONArray streamsArr = streamsJSON.getJSONArray(Constants.JSON_FIELD_STREAMS);			
			season.getStreams().fromJSON(streamsArr, streamsDB);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season streams '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	public static String initFromSeasonNotificationsJSON(JSONObject notificationsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

		String seasonId = notificationsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			season.getNotifications().fromJSON(notificationsJSON, notificationsDB);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season streams  '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	public static String initFromSeasonStreamsEventsJSON(JSONObject streamsEventsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		String seasonId = streamsEventsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			season.getStreamsEvents().fromJSON(streamsEventsJSON);

			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season streams '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	public static String initFromSeasonOriginalStringsJSON(JSONObject originalStringsJSON, ServletContext context) throws JSONException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String pathSeparator = ds.getSeparator();

		String seasonId = originalStringsJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			//for now no validation is done - error will cause JSONError.
			//JSONArray origStrsSArr = (JSONArray) originalStringsJSON.get(Constants.JSON_FIELD_STRINGS);
			//season.getOriginalStrings().fromJSON(origStrsSArr, originalStringsDB);
			season.getOriginalStrings().fromJSON(originalStringsJSON, originalStringsDB);

			String oldEnglishStringsFilePath = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+
					pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.TRANSLATIONS_FOLDER_NAME + 
					pathSeparator + Constants.STRINGS_FILE_NAME_PREFIX + Constants.DEFAULT_LANGUAGE + Constants.STRINGS_FILE_NAME_EXTENSION;

			//if strings__en.json file exist which means it is an old season - set its content as the strings__enDEVELOPMENT.json content		
			if (ds.isFileExists(oldEnglishStringsFilePath)) {
				season.getOriginalStrings().setOldEnStringsFileExists(true);					
				if (originalStringsJSON.containsKey(Constants.JSON_FIELD_SUPPORTED_LANGUAGES)) {
					//this is an indication that this pre 2.1 season was upgraded
					season.getOriginalStrings().setUpgradedPre21Season(true);
				}
			}


			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + jsne.getMessage());
		}
	}

	//return null if no such stage
	public static Stage strToStage(String stageStr) {		
		if (stageStr == null) 
			return null;

		if(stageStr.equalsIgnoreCase(Stage.DEVELOPMENT.toString())) 
			return Stage.DEVELOPMENT;


		if(stageStr.equalsIgnoreCase(Stage.PRODUCTION.toString())) 
			return Stage.PRODUCTION;

		return null; 
	}

	//return null if no such stage
	public static BranchStatus strToBranchStatus(String branchStatusStr) {		
		if (branchStatusStr == null) 
			return null;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.CHECKED_OUT.toString())) 
			return BranchStatus.CHECKED_OUT;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.NEW.toString())) 
			return BranchStatus.NEW;

		if(branchStatusStr.equalsIgnoreCase(BranchStatus.NONE.toString())) 
			return BranchStatus.NONE;

		return null; 
	}

	//return null if no such InputSampleGenerationMode
	public static InputSampleGenerationMode strToInputSampleGenerationMode(String generationModeStr) {		
		if (generationModeStr == null) 
			return null;

		if(generationModeStr.equalsIgnoreCase(InputSampleGenerationMode.MAXIMAL.toString())) 
			return InputSampleGenerationMode.MAXIMAL;

		if(generationModeStr.equalsIgnoreCase(InputSampleGenerationMode.MINIMAL.toString())) 
			return InputSampleGenerationMode.MINIMAL;

		if(generationModeStr.equalsIgnoreCase(InputSampleGenerationMode.PARTIAL.toString())) 
			return InputSampleGenerationMode.PARTIAL;

		return null; 
	}

	//return null if no such StringsOutputMode
	public static StringsOutputMode strToStringsOutputMode(String stringsOutputModeStr) {		
		if (stringsOutputModeStr == null) 
			return null;

		if(stringsOutputModeStr.equalsIgnoreCase(StringsOutputMode.BASIC.toString())) 
			return StringsOutputMode.BASIC;

		if(stringsOutputModeStr.equalsIgnoreCase(StringsOutputMode.INCLUDE_TRANSLATIONS.toString())) 
			return StringsOutputMode.INCLUDE_TRANSLATIONS;

		return null; 
	}

	//return null if no such BranchesOutputMode
	public static BranchesOutputMode strToBranchesOutputMode(String branchesOutputModeStr) {	
		return valueOf(BranchesOutputMode.class, branchesOutputModeStr);
	}

	//return null if no such CancelCheckoutMode
	public static CancelCheckoutMode strToCancelCheckoutMode(String cancelCheckoutModeStr) {		
		if (cancelCheckoutModeStr == null) 
			return null;

		if(cancelCheckoutModeStr.equalsIgnoreCase(CancelCheckoutMode.STAND_ALONE.toString())) 
			return CancelCheckoutMode.STAND_ALONE;

		if(cancelCheckoutModeStr.equalsIgnoreCase(CancelCheckoutMode.INCLUDE_SUB_FEATURES.toString())) 
			return CancelCheckoutMode.INCLUDE_SUB_FEATURES;

		return null; 
	}

	//return null if no such stage
	public static SimulationType strToSimulationType(String simulationTypeStr) {		
		if (simulationTypeStr == null) 
			return null;

		if(simulationTypeStr.equalsIgnoreCase(SimulationType.RULE.toString())) 
			return SimulationType.RULE;


		if(simulationTypeStr.equalsIgnoreCase(SimulationType.CONFIGURATION.toString())) 
			return SimulationType.CONFIGURATION;

		return null; 
	}

	//return null if no such type
	public static ActionType strToActionType(String actionTypeStr, ActionType defaultActionType) {
		if (actionTypeStr == null)
			return defaultActionType;
		
		return valueOf(ActionType.class, actionTypeStr);
	}

	//return null if no such stage
	public static InputFormat strToInputFormat(String inputFormatStr) {
		if (inputFormatStr == null)
			return null;

		if(inputFormatStr.equalsIgnoreCase(InputFormat.ANDROID.toString()))
			return InputFormat.ANDROID;

		if(inputFormatStr.equalsIgnoreCase(InputFormat.IOS.toString()))
			return InputFormat.IOS;

		return null;
	}

	public static String streamToString(InputStream is)
	{
		java.util.Scanner s = new java.util.Scanner(is, "UTF-8");
		s.useDelimiter("\\A");
		String out = s.hasNext() ? s.next() : "";
		s.close();
		return out;
	}
	public static JSONObject readJson(String filepath) throws IOException, JSONException
	{
		String str = readString(filepath);
		return new JSONObject(str);
	}
	public static String readString(String filepath) throws IOException
	{
		Path path = Paths.get(filepath);
		byte[] data = Files.readAllBytes(path);
		return new String(data, "UTF-8");
	}
	public static void writeString(String content, String path) throws FileNotFoundException, UnsupportedEncodingException
	{
		try (  PrintWriter out = new PrintWriter(path, "UTF-8")  )
		{
			out.println(content);
		}
	}
	//if valid return null, else return the error in output String
	static public String validateInputSchemaAndDefinitionName (String inName) {
		if (inName == null || inName.isEmpty())
			return "The name field cannot be null or empty.";

		if (!Character.isLetter(inName.charAt(0))) {
			return "The name field must start with a letter.";
		}

		for (int i=1; i<inName.length(); i++) {
			Character c = inName.charAt(i);
			if (!Utilities.isEnglishLetter(c) && !Character.isDigit(c) && !c.equals('_')) {
				return "The name field can contain English letters, digits and underscores only.";						
			}			
		}

		return null;		
	}
	public static void mergeJson(JSONObject to, JSONObject from) throws JSONException
	{
		@SuppressWarnings("unchecked")
		Set<String> keys = from.keySet();
		for (String key : keys)
		{
			Object obj = from.get(key);
			if (!to.containsKey(key))
			{
				to.put(key, obj);
				continue;
			}

			Object objTo = to.get(key);

			// compare the most derived names
			// this test is too stringent since sometimes we replace a leaf (string) by a tree (JSONObject)
			//String fromClass = obj.getClass().getName();
			//String toClass = objTo.getClass().getName();
			//if (!fromClass.equals(toClass))
			//	throw new JSONException("incompatible json structures can't be merged: " + fromClass + ", " + toClass);

			if (obj instanceof JSONObject && objTo instanceof JSONObject)
			{
				mergeJson((JSONObject)objTo, (JSONObject)obj);
			}
			else // replace leaf item
			{
				to.put(key, obj); // arrays are not concatenated but replaced
			}
		}
	}
	public static Object cloneJson(Object obj, boolean deepClone) throws JSONException
	{
		if (obj instanceof JSONArray)
		{
			JSONArray in = (JSONArray) obj;
			JSONArray out = new JSONArray(in.length());
			for (int i = 0; i < in.length(); ++i)
			{
				Object item = in.get(i);
				if (deepClone)
					item = cloneJson(item, deepClone);
				out.add(item);
			}
			return out;
		}
		if (obj instanceof JSONObject)
		{
			JSONObject in = (JSONObject) obj;
			JSONObject out = new JSONObject();
			@SuppressWarnings("unchecked")
			Set<String> keys = in.keySet();
			for (String key : keys)
			{
				Object item = in.get(key);
				if (deepClone)
					item = cloneJson(item, deepClone);
				out.put(key, item);
			}
			return out;
		}

		//the rest are immmutable so no need to clone (null, Boolean, String, Integer, Double, Short)
		return obj;
	}

	public static String escapeJson(String str)
	{
		if (str == null)
			return "";

		StringBuilder b = new StringBuilder();
		b.append('"');

		for (int i = 0; i < str.length(); ++i)
		{
			char c = str.charAt(i);
			switch (c)
			{
			case '\b': b.append("\\b"); break;
			case '\t': b.append("\\t"); break;
			case '\n': b.append("\\n"); break;
			case '\f': b.append("\\f"); break;
			case '\r': b.append("\\r"); break;

			case '\\':
			case '/':
			case '"':
				b.append('\\');
				b.append(c);
				break;

			default:
				if (c < ' ') {
					b.append(inHex(c));
				}
				else
					b.append(c);
			}
		}

		b.append('"');
		return b.toString();
	}
	static String inHex(char c)
	{
		String str = "000" + Integer.toHexString(c);
		return "\\u" + str.substring(str.length() - 4);
	}
	// this also removes everything that follows an unbalanced /*
	public static String removeComments(String text, boolean keepQuotes)
	{
		if (text.length() <= 1)
			return text;

		boolean multiLine = false;
		boolean singleLine = false;
		char inQuotes = 0; // for 'singleQuotes', "doubleQuotes" and /RegularExpression/

		// TODO:  /RegularExpression/ isn't processed, since a lexer is needed to distinguish it from a division operator.
		// temporary workaround: use RegExp("aaa") instead of /aaa/

		StringBuilder b = new StringBuilder();
		int upto = text.length() - 1;
		int i = 0;

		while (i < upto)
		{
			char c = text.charAt(i);
			char next = text.charAt(i+1);
			if (singleLine)
			{
				if (c == '\r' || c == '\n')
				{
					singleLine = false;
					b.append(c);
				}
				++i;
			}
			else if (multiLine)
			{
				if (c == '*' && next == '/')
				{
					multiLine = false;
					i += 2;
				}
				else
					++i;
			}
			else if (inQuotes != 0)
			{
				if (c == '\\') // && next == inQuotes)
				{
					i += 2;
					if (keepQuotes)
					{
						b.append(c);
						b.append(next);
					}
				}
				else
				{
					++i;
					if (keepQuotes || c == inQuotes)
						b.append(c);

					if (c == inQuotes)
						inQuotes = 0;
				}
			}
			else if (c == '/' && next == '/')
			{
				singleLine = true;
				i += 2;
			}
			else if (c == '/' && next == '*')
			{
				multiLine = true;
				i += 2;
			}
			else
			{
				b.append(c);
				++i;
				if (c == '"' || c == '\'')
					inQuotes = c;
			}
		}

		// add last char
		if (i == upto && !singleLine && !multiLine)
			b.append(text.charAt(i));

		return b.toString();
	}
	public static class Props extends Properties
	{
		private static final long serialVersionUID = 1L;

		public void load(String path) throws UnsupportedEncodingException, FileNotFoundException, IOException
		{
			super.load(new InputStreamReader(new FileInputStream(path), "UTF-8"));
		}
		public String get(String key) throws Exception
		{
			String value = super.getProperty(key);
			if (value == null)
				throw new Exception("Missing key in properties file: " + key);
			return value;
		}
		public String get(String key, String defaultValue)
		{
			String value = super.getProperty(key);
			return (value == null) ? defaultValue : value;
		}
		public String getOptional(String key)
		{
			return super.getProperty(key); // null if missing
		}
	}

	//return informative String if id in use, null if not
	public static String verifyGUIDIsNotInUse(ServletContext context, String id) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);		
		@SuppressWarnings("unchecked")
		Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

		Product prod = productsDB.get(id);		
		if (prod!=null)
			return "The specified id '" + id + "' is in use by the product '" + prod.getName() + "'"; 

		Season season = seasonsDB.get(id);		
		if (season!=null)
			return "The specified id '" + id + "' is in use by a version range."; 

		BaseAirlockItem alItem = airlockItemsDB.get(id);
		if (alItem!=null)
			return "The specified id '" + id + "' is in use by a feature, configuration, or mutual exclusion group.";

		AirlockUtility util = utilitiesDB.get(id);
		if (util!=null)
			return "The specified id '" + id + "' is in use by a utility.";

		OriginalString origStr = originalStringsDB.get(id);
		if (origStr!=null)
			return "The specified id '" + id + "' is in use by the string '" + origStr.getKey() + "'";

		return null;		
	}

	public static LinkedList<AirlockChangeContent> touchProductionChangedFile(ServletContext context, DataSerializer ds, Season season) throws IOException {
		//ds.writeData(season.getProductionChangedFilePath(ds.getSeparator()), UUID.randomUUID().toString(), true);
		AirlockFilesWriter.writeData(context, ds, season.getProductionChangedFilePath(ds.getSeparator()), UUID.randomUUID().toString(), season, true);
		LinkedList<AirlockChangeContent> toRet = new LinkedList<AirlockChangeContent>();
		toRet.add(AirlockChangeContent.getAdminChange(UUID.randomUUID().toString(), season.getProductionChangedFilePath(ds.getSeparator()), Stage.PRODUCTION));
		return toRet;
	}

	public static Map<String, BaseAirlockItem> getAirlockItemsMapCopy (ServletContext context, String seasonId) throws IOException {
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		
		Map<String, BaseAirlockItem> copyItemsMap = new ConcurrentHashMap<String, BaseAirlockItem>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String pathSeparator = ds.getSeparator();
		
		String seasonFeaturesFile = Constants.SEASONS_FOLDER_NAME + pathSeparator + seasonsDB.get(seasonId).getProductId().toString() 
				+ pathSeparator + seasonId+pathSeparator + Constants.AIRLOCK_FEATURES_FILE_NAME;
		
		try {
			JSONObject featuresJSON = ds.readDataToJSON(seasonFeaturesFile);
			initFromSeasonFeaturesJSON(featuresJSON, seasonsDB, copyItemsMap, ds, false, Constants.REQUEST_ITEM_TYPE.FEATURES); //only create new features db - don't update season
			
			if ( seasonsDB.get(seasonId).getSeasonCapabilities(context).contains(AirlockCapability.ENTITLEMENTS)) {
				String seasonEntitlementsFile = Constants.SEASONS_FOLDER_NAME + pathSeparator + seasonsDB.get(seasonId).getProductId().toString() 
						+ pathSeparator + seasonId+pathSeparator + Constants.AIRLOCK_ENTITLEMENTS_FILE_NAME;
				
				JSONObject entitlementsJSON = ds.readDataToJSON(seasonEntitlementsFile);
				initFromSeasonFeaturesJSON(entitlementsJSON, seasonsDB, copyItemsMap, ds, false, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); //only add entitlements to db - don't update season
			}
			else {
				copyItemsMap.put(seasonsDB.get(seasonId).getEntitlementsRoot().getUniqueId ().toString(), seasonsDB.get(seasonId).getEntitlementsRoot());
			}
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		} catch (JSONException e) {
			String errMsg = seasonFeaturesFile + " file is not in a legal JSON format: " + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);		
		}			

		return copyItemsMap;
	}

	public static Map<String, BaseAirlockItem> getAirlockItemsMapCopyForSeason (ServletContext context, String seasonId) throws IOException {
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Map<String, BaseAirlockItem> copyItemsMap = new ConcurrentHashMap<String, BaseAirlockItem>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String pathSeparator = ds.getSeparator();

		String seasonFeaturesFile = Constants.SEASONS_FOLDER_NAME + pathSeparator + seasonsDB.get(seasonId).getProductId().toString() 
				+ pathSeparator + seasonId+pathSeparator + Constants.AIRLOCK_FEATURES_FILE_NAME;
		try {
			JSONObject featuresJSON = ds.readDataToJSON(seasonFeaturesFile);
			initFromSeasonFeaturesJSON(featuresJSON, seasonsDB, copyItemsMap, ds, false, Constants.REQUEST_ITEM_TYPE.FEATURES); //only create new features db - don't update season
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		} catch (JSONException e) {
			String errMsg = seasonFeaturesFile + " file is not in a legal JSON format: " + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);		
		}			


		return copyItemsMap;

	}

	public static ValidationResults copy(Map<String, BaseAirlockItem> airlockItemsMapCopy, BaseAirlockItem featureToCopy, String newParentId, String nameSuffix, String minAppVersion,
			InternalUserGroups userGroups, UserInfo userInfo, ServletContext context, Map<String, String> oldToNewIDsMap, Environment env,List<OriginalString> copiedStings,
			Branch destinationBranchCopy, Map<String, BaseAirlockItem> sourceAirlockItemsDB, Season srcSeason, REQUEST_ITEM_TYPE itemType) {
		
		ValidationCache tester = new ValidationCache(null, copiedStings); // cache with changed strings

		return doCopy(airlockItemsMapCopy, featureToCopy, newParentId, nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
	}
	
	public static ValidationResults doCopy(Map<String, BaseAirlockItem> airlockItemsMapCopy, BaseAirlockItem featureToCopy, String newParentId, String nameSuffix, String minAppVersion,
			InternalUserGroups userGroups, UserInfo userInfo, ServletContext context, Map<String, String> oldToNewIDsMap, Environment env,List<OriginalString> copiedStings,
			ValidationCache tester,
			Branch destinationBranchCopy, Map<String, BaseAirlockItem> sourceAirlockItemsDB, REQUEST_ITEM_TYPE itemType) {

		try {
			//go over all the features to copy validate and add one by one (without children) using the copy map.
			BaseAirlockItem parentFeature = airlockItemsMapCopy.get(newParentId);
			JSONObject featureToCopyJSON = featureToCopy.toJson(OutputJSONMode.ADMIN, context, env);
			String prevId = featureToCopyJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);
			featureToCopyJSON.remove(Constants.JSON_FIELD_UNIQUE_ID);
			featureToCopyJSON.remove(Constants.JSON_FIELD_LAST_MODIFIED);
			featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_STATUS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS);

			featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, parentFeature.getSeasonId().toString());
			if (featureToCopy instanceof DataAirlockItem) { 
				featureToCopyJSON.put(Constants.JSON_FIELD_NAME, ((DataAirlockItem)featureToCopy).getName()+nameSuffix);
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_STAGE, Stage.DEVELOPMENT.toString()); //move copied features to dev
			}

			if (featureToCopy.getFeaturesItems() != null) 
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_FEATURES);

			if (featureToCopy.getConfigurationRuleItems()!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);

			if (featureToCopy.getOrderingRuleItems()!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
			
			if (featureToCopy.getEntitlementItems()!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);

			if (featureToCopy.getPurchaseOptionsItems()!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);

			if (minAppVersion!=null)
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);

			if (featureToCopy instanceof OrderingRuleItem) {
				//fix the configuration - replace the subFeatures ids with the new ids
				String newConfig = OrderingRuleItem.replaceIdsInConfiguration(tester, context, oldToNewIDsMap, 
						((OrderingRuleItem)featureToCopy).getConfiguration(), ((OrderingRuleItem)featureToCopy).getRule(), 
						((OrderingRuleItem)featureToCopy).getSeasonId().toString(), ((OrderingRuleItem)featureToCopy).getStage(), 
						((OrderingRuleItem)featureToCopy).getMinAppVersion());
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, newConfig);
			}

			BaseAirlockItem newAirlockObj = BaseAirlockItem.getAirlockItemByType(featureToCopyJSON);
			if (newAirlockObj == null) {
				//should never happen!
				String errMsg = Strings.typeNotFound;
				logger.severe(errMsg);
				return null;
			}

			LinkedList<String> addedSubFeatures = new LinkedList<String>();
			LinkedList<String> missingSubFeatures = new LinkedList<String>();

			//check that each featureId appears only once in the update tree
			HashMap<UUID, Integer> existingFeaturesInUpdate = new HashMap<UUID, Integer>();  

			HashMap<String, JSONObject> updatedFeatures = new HashMap<String, JSONObject>();

			//if is premium feature - verify that entitlement exists in the destination
			if (featureToCopy.getType().equals(Type.FEATURE)) {
				String entitlementId = ((FeatureItem) featureToCopy).getEntitlement();
				if (entitlementId!=null && !entitlementId.isEmpty()) {
					EntitlementItem entitlementInSource = (EntitlementItem)sourceAirlockItemsDB.get(entitlementId);
					EntitlementItem entitlementInDestination = Utilities.getEntitlementByNameStageAndSeason(airlockItemsMapCopy, entitlementInSource.getNameSpaceDotName(), parentFeature.getSeasonId(), ((FeatureItem) featureToCopy).getStage());
					if (entitlementInDestination == null) {
						return new ValidationResults(String.format(Strings.cannotCopyMissingEntitlement, entitlementInSource.getNameSpaceDotName(), featureToCopy.getNameSpaceDotName()), Status.BAD_REQUEST);
					}
					featureToCopyJSON.put(Constants.JSON_FIELD_ENTITLEMENT, entitlementInDestination.getUniqueId().toString());
				}
			}
			
			//if is bundle - verify that includedEntitlements exists in the destination
			if (featureToCopy.getType().equals(Type.ENTITLEMENT)) {
				LinkedList<String> includedEntitlements = ((EntitlementItem) featureToCopy).getIncludedPurchases();
				JSONArray newIncludedEntitlementsArr = new JSONArray();
				for (int j =0; j<includedEntitlements.size(); j++) {
					String entitlementId = includedEntitlements.get(j);
					if (entitlementId!=null && !entitlementId.isEmpty()) {
						EntitlementItem entitlementInSource = (EntitlementItem)sourceAirlockItemsDB.get(entitlementId);
						EntitlementItem entitlementInDestination = Utilities.getEntitlementByNameStageAndSeason(airlockItemsMapCopy, entitlementInSource.getNameSpaceDotName(), parentFeature.getSeasonId(), Stage.valueOf(featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE)));
						if (entitlementInDestination == null) {
							return new ValidationResults(String.format(Strings.cannotImportMissingIncludedEntitlement, entitlementInSource.getNameSpaceDotName(), featureToCopyJSON.getString(Constants.JSON_FIELD_NAME)), Status.BAD_REQUEST);
						}
						newIncludedEntitlementsArr.add(entitlementInDestination.getUniqueId().toString());
					}
				}
				featureToCopyJSON.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, newIncludedEntitlementsArr);
			}

			ValidationResults validationRes = newAirlockObj.doValidateFeatureJSON(featureToCopyJSON, context, parentFeature.getSeasonId().toString(), addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, newParentId, updatedFeatures, userInfo, tester, airlockItemsMapCopy, env,copiedStings);
			if (validationRes!=null) 
				return validationRes;

			newAirlockObj.fromJSON(featureToCopyJSON, airlockItemsMapCopy, UUID.fromString(newParentId), env);
			newAirlockObj.setUniqueId(UUID.randomUUID());
			oldToNewIDsMap.put(prevId, newAirlockObj.getUniqueId().toString());
			if (!env.isInMaster()) {
				//add feature to branch.								
				String err = destinationBranchCopy.addAirlockItem (newAirlockObj, parentFeature, itemType);
				//err = parentFeature.addAirlockItem(newAirlockObj);
				parentFeature.setLastModified(new Date());
				if (err!=null) {
					logger.severe(err);
					return new ValidationResults(err, Status.BAD_REQUEST);
				}
				airlockItemsMapCopy.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
				newAirlockObj.setBranchStatus(BranchStatus.NEW);
			}
			else {
				String err = parentFeature.addAirlockItem(newAirlockObj);
				if (err!=null) {
					logger.severe(err);
					return new ValidationResults (err, Status.BAD_REQUEST);
				}
				parentFeature.setLastModified(new Date());

				airlockItemsMapCopy.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
			}

			if (featureToCopy.getConfigurationRuleItems()!=null) {
				for (int i=0; i<featureToCopy.getConfigurationRuleItems().size(); i++) {
					BaseAirlockItem subFeatureToCopy = featureToCopy.getConfigurationRuleItems().get(i);			
					validationRes = doCopy(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
					if (validationRes!=null) 
						return validationRes;				
				}
			}

			if (featureToCopy.getFeaturesItems()!=null) {
				for (int i=0; i<featureToCopy.getFeaturesItems().size(); i++) {
					BaseAirlockItem subFeatureToCopy = featureToCopy.getFeaturesItems().get(i);			
					validationRes = doCopy(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
					if (validationRes!=null) 
						return validationRes;
				}
			}

			if (featureToCopy.getOrderingRuleItems()!=null) {
				for (int i=0; i<featureToCopy.getOrderingRuleItems().size(); i++) {
					BaseAirlockItem subFeatureToCopy = featureToCopy.getOrderingRuleItems().get(i);			
					validationRes = doCopy(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
					if (validationRes!=null) 
						return validationRes;
				}
			}
			
			if (featureToCopy.getEntitlementItems()!=null) {
				for (int i=0; i<featureToCopy.getEntitlementItems().size(); i++) {
					BaseAirlockItem subFeatureToCopy = featureToCopy.getEntitlementItems().get(i);			
					validationRes = doCopy(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
					if (validationRes!=null) 
						return validationRes;
				}
			}
			
			if (featureToCopy.getPurchaseOptionsItems()!=null) {
				for (int i=0; i<featureToCopy.getPurchaseOptionsItems().size(); i++) {
					BaseAirlockItem subFeatureToCopy = featureToCopy.getPurchaseOptionsItems().get(i);			
					validationRes = doCopy(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, oldToNewIDsMap, env,copiedStings, tester, destinationBranchCopy, sourceAirlockItemsDB, itemType);
					if (validationRes!=null) 
						return validationRes;
				}
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);			
		}
		return null;
	}

	private static EntitlementItem getEntitlementByNameStageAndSeason(Map<String, BaseAirlockItem> airlockItemsMapCopy, String entitlementNamespaceDotName, UUID seasonId, Stage stage) {
		Set<String> itemIds = airlockItemsMapCopy.keySet();
		for (String id:itemIds) {
			BaseAirlockItem alItem = airlockItemsMapCopy.get(id);
			if (alItem instanceof EntitlementItem && 
					alItem.getSeasonId().equals(seasonId) && 
					((EntitlementItem)alItem).getNameSpaceDotName().equals(entitlementNamespaceDotName) &&
					(((EntitlementItem) alItem).getStage().equals(Stage.PRODUCTION) || stage.equals(Stage.DEVELOPMENT))) {
				return (EntitlementItem)alItem;
			}
		}
		return null;
	}
	public static JSONObject getDirectAssetsViolations(Season sourceSeason,Season destSeason,BaseAirlockItem item,ServletContext context) throws Exception{
		JSONArray missingUtils =  getDirectUtilitiesViolations(sourceSeason,destSeason,item);
		JSONArray missingFields = getDirectFieldsViolations(destSeason,item,context);
		if(missingFields.size() != 0 || missingUtils.size() != 0){
			JSONObject violations = new JSONObject();
			violations.put("missingUtilities",missingUtils);
			violations.put("missingFields",missingFields);
			return violations;
		}
		return null;
	}

	public static JSONObject getDirectAssetsViolationsJSON(Season destSeason,JSONObject item,ServletContext context) throws Exception{
		JSONArray missingUtils =  new JSONArray();//getDirectUtilitiesViolations(sourceSeason,destSeason,item);
		JSONArray missingFields = getDirectFieldsViolationsJSON(destSeason,item,context);
		if(missingFields.size() != 0 || missingUtils.size() != 0){
			JSONObject violations = new JSONObject();
			violations.put("missingUtilities",missingUtils);
			violations.put("missingFields",missingFields);
			return violations;
		}
		return null;
	}

	public static JSONArray getDirectUtilitiesViolations(Season sourceSeason,Season destSeason,BaseAirlockItem item) throws JSONException {
		JSONArray missingUtils = new JSONArray();

		//get all utility in use for this item
		List<AirlockUtility> utilitiesList = sourceSeason.getUtilities().utilitiesList;
		StringBuilder allUtils = new StringBuilder();
		for(int i = 0;i<utilitiesList.size();++i){
			allUtils.append(utilitiesList.get(i).getUtility());
			allUtils.append("\n");
		}
		Map<String,int[]> allFuncMap = VerifyRule.findFunctions(allUtils.toString());
		Set<String> allFuncSet = allFuncMap.keySet();
		Set<String> funcInUse  = findFunctionsInItem(allFuncSet,item);
		// check if they exist in dest season

		List<AirlockUtility> utilitiesListDest = destSeason.getUtilities().utilitiesList;
		StringBuilder allUtilsDest = new StringBuilder();
		for(int i = 0;i<utilitiesListDest.size();++i){
			allUtilsDest.append(utilitiesListDest.get(i).getUtility());
			allUtilsDest.append("\n");
		}
		Set<String> functionInDest  =VerifyRule.findFunctionsInRule(funcInUse,allUtilsDest.toString());
		Iterator<String> iter = funcInUse.iterator();
		while (iter.hasNext()){
			String func = iter.next();
			if(!functionInDest.contains(func)) {
				missingUtils.add(func);
			}
		}
		return missingUtils;
	}

	public static Set<String> findFunctionsInItem(Set<String> functions, BaseAirlockItem item){
		StringBuilder rulesAndConfig = new StringBuilder();
		getRulesAndConfig(item,rulesAndConfig);
		return VerifyRule.findFunctionsInRule(functions,rulesAndConfig.toString());
	}

	public static void getRulesAndConfig(BaseAirlockItem item,StringBuilder builder){
		if(item instanceof DataAirlockItem){
			String ruleString = ((DataAirlockItem) item).getRule().getRuleString();
			builder.append(ruleString);
			builder.append("\n");
		}
		if(item instanceof FeatureItem){
			if (((FeatureItem)item).getPremiumRule()!=null) {
				String ruleString = ((FeatureItem) item).getPremiumRule().getRuleString();
				builder.append(ruleString);
				builder.append("\n");
			}
		}
		if(item instanceof ConfigurationRuleItem){
			String configString = ((ConfigurationRuleItem) item).getConfiguration();
			builder.append(configString);
			builder.append("\n");
		}
		LinkedList<BaseAirlockItem> featuresItems = item.getFeaturesItems();
		if(featuresItems !=null) {
			for (int i = 0; i < featuresItems.size(); ++i) {
				getRulesAndConfig(featuresItems.get(i), builder);
			}
		}
		LinkedList<BaseAirlockItem> configsItems = item.getConfigurationRuleItems();
		if(configsItems != null) {
			for (int i = 0; i < configsItems.size(); ++i) {
				getRulesAndConfig(configsItems.get(i), builder);
			}
		}
		LinkedList<BaseAirlockItem> entitlementItems = item.getEntitlementItems();
		if(entitlementItems !=null) {
			for (int i = 0; i < entitlementItems.size(); ++i) {
				getRulesAndConfig(entitlementItems.get(i), builder);
			}
		}
		LinkedList<BaseAirlockItem> purchaseOptionsItems = item.getPurchaseOptionsItems();
		if(purchaseOptionsItems !=null) {
			for (int i = 0; i < purchaseOptionsItems.size(); ++i) {
				getRulesAndConfig(purchaseOptionsItems.get(i), builder);
			}
		}
	}
 
	public static void getRulesAndConfigJSON(JSONObject item,StringBuilder builder)throws JSONException{
		if(item.has(Constants.JSON_FEATURE_FIELD_RULE)){
			String ruleString = item.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE).getString(Constants.JSON_RULE_FIELD_RULE_STR);
			builder.append(ruleString);
			builder.append("\n");
		}
		if(item.has(Constants.JSON_FEATURE_FIELD_CONFIGURATION)){
			String configString = item.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
			builder.append(configString);
			builder.append("\n");
		}
		
		if (item.has(Constants.JSON_FEATURE_FIELD_FEATURES)) {
			JSONArray featuresItems = item.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
			if(featuresItems !=null) {
				for (int i = 0; i < featuresItems.size(); ++i) {
					getRulesAndConfigJSON(featuresItems.getJSONObject(i), builder);
				}
			}
		}
		if (item.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
			JSONArray entitlementsItems = item.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
			if(entitlementsItems !=null) {
				for (int i = 0; i < entitlementsItems.size(); ++i) {
					getRulesAndConfigJSON(entitlementsItems.getJSONObject(i), builder);
				}
			}
		}
		if (item.has(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)) {
			JSONArray purchaseOptionsItems = item.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);
			if(purchaseOptionsItems !=null) {
				for (int i = 0; i < purchaseOptionsItems.size(); ++i) {
					getRulesAndConfigJSON(purchaseOptionsItems.getJSONObject(i), builder);
				}
			}
		}
		if (item.has(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)) {
			JSONArray configsItems = item.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
			if(configsItems != null) {
				for (int i = 0; i < configsItems.size(); ++i) {
					getRulesAndConfigJSON(configsItems.getJSONObject(i), builder);
				}
			}
		}
	}

	public static JSONArray getDirectFieldsViolations(Season season,BaseAirlockItem item,ServletContext context) throws Exception {
		StringBuilder rulesAndConfig = new StringBuilder();
		getRulesAndConfig(item,rulesAndConfig);
		ValidationCache validationCache = new ValidationCache();
		ValidationCache.Info info = validationCache.getInfo(context, season, Stage.DEVELOPMENT,  "1000");
		return VerifyRule.findMissingFields(info.maximalInvoker, rulesAndConfig.toString());
	}

	public static JSONArray getDirectFieldsViolationsJSON(Season season,JSONObject item,ServletContext context) throws Exception {
		StringBuilder rulesAndConfig = new StringBuilder();
		getRulesAndConfigJSON(item,rulesAndConfig);
		ValidationCache validationCache = new ValidationCache();
		ValidationCache.Info info = validationCache.getInfo(context, season, Stage.DEVELOPMENT,  "1000");
		return VerifyRule.findMissingFields(info.maximalInvoker, rulesAndConfig.toString());
	}


	public static JSONObject getNameAndMinAppVerAndAssetsViolation(Map<String, BaseAirlockItem> sourceAirlockItemsDB,
			Map<String, BaseAirlockItem> destinationAirlockItemsDB,
			String featureId, String newParentId, String nameSuffix, String givenMinAppVersion, Map<String, Season> seasonsDB, 
			Season season,List<OriginalString> copiedStrings, ServletContext context, String destinationBranchId) throws JSONException {

		JSONObject resultsJson = new JSONObject();

		//BaseAirlockItem parentFeature = airlockItemsDB.get(newParentId); //as this stage i know that the parent feature exists
		//UUID parentSeasonID = parentFeature.getSeasonId();
		//Season season = seasonsDB.get(parentSeasonID.toString());
		String parentSeasonMaxVer = season.getMaxVersion();						

		LinkedList<JSONObject> illegalFeatureMinApp = new LinkedList<JSONObject>();
		LinkedList<JSONObject> illegalFeatureName = new LinkedList<JSONObject>();
		LinkedList<JSONObject> missingAssets = new LinkedList<JSONObject>();

		boolean errorFound = false;

		//if the user specify a minAppVersion to all of the copied features - you can validate it in advanced 
		if (givenMinAppVersion!=null && parentSeasonMaxVer!=null) {
			if (Season.compare(givenMinAppVersion, parentSeasonMaxVer)>=0) { 
				resultsJson.put(Constants.JSON_FIELD_ILLEGAL_GIVEN_MIN_APP_VER, "The Minimum App Version exceeds the maximum version of the parent's version range."); 
				errorFound = true;
			}
			parentSeasonMaxVer = null; //this means that minAppVersion validation is not necessary
		}

		ValidationCache tester = new ValidationCache(null, copiedStrings); // cache with changed translations
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion()); 		

		doGetNameAndMinAppVerAndAssetsViolations (sourceAirlockItemsDB, destinationAirlockItemsDB, featureId, parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, 
				illegalFeatureName, missingAssets, destinationAirlockItemsDB.get(season.getRoot().getUniqueId().toString()), season, context, tester, 
				copiedStrings, env, destinationBranchId);

		doGetNameAndMinAppVerAndAssetsViolations (sourceAirlockItemsDB, destinationAirlockItemsDB, featureId, parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, 
				illegalFeatureName, missingAssets, destinationAirlockItemsDB.get(season.getEntitlementsRoot().getUniqueId().toString()), season, context, tester, 
				copiedStrings, env, destinationBranchId);

		if (illegalFeatureMinApp.size()>0 || illegalFeatureName.size()>0 || missingAssets.size()>0 || errorFound) {
			JSONArray illegalFeatureMinAppArray = new JSONArray();
			JSONArray illegalFeatureNameArray = new JSONArray();

			for (int i=0; i<illegalFeatureMinApp.size(); i++)
				illegalFeatureMinAppArray.add(illegalFeatureMinApp.get(i));

			for (int i=0; i<illegalFeatureName.size(); i++)
				illegalFeatureNameArray.add(illegalFeatureName.get(i));


			resultsJson.put(Constants.JSON_FIELD_ILLEGAL_MIN_APP_VER, illegalFeatureMinAppArray);
			resultsJson.put(Constants.JSON_FIELD_ILLEGAL_NAME, illegalFeatureName);
			resultsJson.put(Constants.JSON_FIELD_MISSING_ASSETS, missingAssets);
			errorFound = true;
		}

		if (errorFound)
			return resultsJson;

		return null;
	}

	private static void doGetNameAndMinAppVerAndAssetsViolations(Map<String, BaseAirlockItem> sourceAirlockItemsDB,
			Map<String, BaseAirlockItem> destinationAirlockItemsDB,
			String featureId, String parentSeasonMaxVer, String nameSuffix, 
			LinkedList<JSONObject> illegalFeatureMinApp, LinkedList<JSONObject> illegalFeatureName, LinkedList<JSONObject> missingAssets,
			BaseAirlockItem root, Season season, ServletContext context,
			ValidationCache tester,
			List<OriginalString> copiedStrings, Environment env,
			String destinationBranchId) throws JSONException {

		BaseAirlockItem curItem = sourceAirlockItemsDB.get(featureId);
		if (curItem == null) //should never happen
			return;

		if (curItem instanceof DataAirlockItem) {
			DataAirlockItem curDataAirlockItem = (DataAirlockItem)curItem;
			if (parentSeasonMaxVer!=null && (Season.compare(curDataAirlockItem.getMinAppVersion(), parentSeasonMaxVer)>=0)) {  
				JSONObject illegalFeatureJSON = new  JSONObject();
				illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, ((DataAirlockItem)curItem).getNameSpaceDotName());
				illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curItem.getUniqueId().toString());
				illegalFeatureMinApp.add(illegalFeatureJSON);
			}

			if (DataAirlockItem.verifyNamespaceNameUniqueness(curDataAirlockItem.getNamespace(), curDataAirlockItem.getName() + nameSuffix, root, null, curItem.getType()) != null) {
				JSONObject illegalFeatureJSON = new  JSONObject();
				illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, curItem.getNameSpaceDotName());
				illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curItem.getUniqueId().toString());
				illegalFeatureName.add(illegalFeatureJSON);
			}
			else {
				String namespaceDotName = curItem.getNameSpaceDotName()+nameSuffix;

				if (destinationBranchId!=null && destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
					//when copying to master we should verify that this feature does not exists in one of it branches
					boolean found = false;
					for (Branch b:season.getBranches().getBranchesList()) {
						Set<String> branchesFeatures = b.getBranchAirlockItemsBD().keySet();
						for (String branchFeatureId:branchesFeatures) {				
							BaseAirlockItem branchAI = b.getBranchAirlockItemsBD().get(branchFeatureId);
							if (branchAI.getBranchStatus().equals(BranchStatus.NEW) && branchAI instanceof DataAirlockItem) {
								String nsDotName = ((DataAirlockItem)(branchAI)).getNameSpaceDotName();
								if (namespaceDotName.equals(nsDotName)) {
									JSONObject illegalFeatureJSON = new  JSONObject();
									illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, curItem.getNameSpaceDotName());
									illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curItem.getUniqueId().toString());
									illegalFeatureName.add(illegalFeatureJSON);
									found=true;
									break;
								}
							}
							if (found) {
								break;
							}
						}
					}
				}
				else { // if importing to branch - verify name uniqueness within branch
					if (destinationBranchId!=null && !destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
						//verify that name is unique in branch
						@SuppressWarnings("unchecked")
						Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

						Branch branch = branchesDB.get(destinationBranchId);
						if (branch.getBranchFeatureByName(namespaceDotName)!=null) {
							JSONObject illegalFeatureJSON = new  JSONObject();
							illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, namespaceDotName);
							illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curItem.getUniqueId().toString());
							illegalFeatureName.add(illegalFeatureJSON);

						}
					}
				}
			}

			//evaluate rule to find missing assets
			if (curDataAirlockItem.getRule()!=null && curDataAirlockItem.getRule().getRuleString()!=null) {

				ValidationResults vr = curDataAirlockItem.getRule().validateRule(Stage.DEVELOPMENT, curDataAirlockItem.getMinAppVersion(), season, context, tester, null);
				if (vr!=null) {
					JSONObject missingAssetsJSON = new  JSONObject();
					missingAssetsJSON.put(Constants.JSON_FIELD_NAME, curDataAirlockItem.getNameSpaceDotName());
					missingAssetsJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curDataAirlockItem.getUniqueId().toString());
					missingAssetsJSON.put(Constants.JSON_FIELD_ERROR, Strings.assetsMissing);
					missingAssets.add(missingAssetsJSON);				
				}								
			}
			if (curDataAirlockItem instanceof ConfigurationRuleItem) {
				ConfigurationRuleItem curConfigItem = (ConfigurationRuleItem) curDataAirlockItem;
				if (curConfigItem.getConfiguration()!=null && !curConfigItem.getConfiguration().isEmpty()) {					
					ValidationResults vr = validateConfigAssets(curConfigItem.getConfiguration(), curConfigItem.getRule().getRuleString(), Stage.DEVELOPMENT, curConfigItem.getMinAppVersion(), season, context, tester, copiedStrings, env);
					curDataAirlockItem.getRule().validateRule(Stage.DEVELOPMENT, curDataAirlockItem.getMinAppVersion(), season, context, tester, null);
					if (vr!=null) {
						JSONObject missingAssetsJSON = new  JSONObject();
						missingAssetsJSON.put(Constants.JSON_FIELD_NAME, curItem.getNameSpaceDotName());
						missingAssetsJSON.put(Constants.JSON_FIELD_UNIQUE_ID, curItem.getUniqueId().toString());
						missingAssetsJSON.put(Constants.JSON_FIELD_ERROR, Strings.assetsMissing);
						missingAssets.add(missingAssetsJSON);				
					}
				}
			}
		}

		if (curItem.getFeaturesItems()!=null) {
			for (int i=0; i<curItem.getFeaturesItems().size(); i++) {
				doGetNameAndMinAppVerAndAssetsViolations(sourceAirlockItemsDB, destinationAirlockItemsDB, curItem.getFeaturesItems().get(i).getUniqueId().toString(),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, missingAssets, root, season , context,
						tester ,copiedStrings, env, destinationBranchId);
			}
		}

		if (curItem.getConfigurationRuleItems()!=null) {
			for (int i=0; i<curItem.getConfigurationRuleItems().size(); i++) {
				doGetNameAndMinAppVerAndAssetsViolations(sourceAirlockItemsDB, destinationAirlockItemsDB, curItem.getConfigurationRuleItems().get(i).getUniqueId().toString(),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, missingAssets, root, season, context,
						tester, copiedStrings, env, destinationBranchId);
			}
		}	
		
		if (curItem.getEntitlementItems()!=null) {
			for (int i=0; i<curItem.getEntitlementItems().size(); i++) {
				doGetNameAndMinAppVerAndAssetsViolations(sourceAirlockItemsDB, destinationAirlockItemsDB, curItem.getEntitlementItems().get(i).getUniqueId().toString(),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, missingAssets, root, season, context,
						tester, copiedStrings, env, destinationBranchId);
			}
		}
		
		if (curItem.getPurchaseOptionsItems()!=null) {
			for (int i=0; i<curItem.getPurchaseOptionsItems().size(); i++) {
				doGetNameAndMinAppVerAndAssetsViolations(sourceAirlockItemsDB, destinationAirlockItemsDB, curItem.getPurchaseOptionsItems().get(i).getUniqueId().toString(),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, missingAssets, root, season, context,
						tester, copiedStrings, env, destinationBranchId);
			}
		}
	}

	private static ValidationResults validateConfigAssets(String configStr, String ruleString, Stage stage, String minAppVesrion, Season season, ServletContext context, 
			ValidationCache tester,
			List<OriginalString> copiedStrings, Environment env) {
		//get relevant en_strings
		try {
			try {
				ValidationCache.Info info = tester.getInfo(context, season, stage, minAppVesrion);
				VerifyRule.fullConfigurationEvaluation(ruleString, configStr, info.minimalInvoker, info.maximalInvoker);
			} catch (ValidationException e) {
				return new ValidationResults("The configuration is invalid: " + e.getMessage(), Status.BAD_REQUEST);		
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		} catch (GenerationException e) {
			return new ValidationResults("Failed to generate a data sample to validate the configuration:" + e.getMessage(), Status.BAD_REQUEST);		
		}

		return null;
	}

	public static JSONObject getNameAndMinAppVersionViolationFromJSON(Map<String, BaseAirlockItem> airlockItemsDB, 
			JSONObject featureJSON, String newParentId, String nameSuffix, String givenMinAppVersion, Map<String, Season> seasonsDB,
			boolean overrideIds,List<OriginalString> copiedStrings, String destinationBranchId, ServletContext context) throws JSONException {

		JSONObject resultsJson = new JSONObject();

		BaseAirlockItem parentFeature = airlockItemsDB.get(newParentId); //as this stage i know that the parent feature exists
		UUID parentSeasonID = parentFeature.getSeasonId();
		Season season = seasonsDB.get(parentSeasonID.toString());
		String parentSeasonMaxVer = season.getMaxVersion();						

		LinkedList<JSONObject> illegalFeatureMinApp = new LinkedList<JSONObject>();
		LinkedList<JSONObject> illegalFeatureName = new LinkedList<JSONObject>();
		LinkedList<JSONObject> illegalFeatureId = new LinkedList<JSONObject>();

		boolean errorFound = false;

		//if the user specify a minAppVersion to all of the copied features - you can validate it in advanced 
		if (givenMinAppVersion!=null && parentSeasonMaxVer!=null) {
			if (Season.compare(givenMinAppVersion, parentSeasonMaxVer)>=0) { 
				resultsJson.put(Constants.JSON_FIELD_ILLEGAL_GIVEN_MIN_APP_VER, "The Minimum App Version exceeds the maximum version of the parent's version range."); 
				errorFound = true;
			}
			parentSeasonMaxVer = null; //this means that minAppVersion validation is not necessary
		}

		doGetNameAndMinAppVerAndIDsViolationsFromJSON (airlockItemsDB, featureJSON, parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, 
				illegalFeatureName, illegalFeatureId, season.getRoot(), overrideIds,copiedStrings, destinationBranchId, season, context);
		
		doGetNameAndMinAppVerAndIDsViolationsFromJSON (airlockItemsDB, featureJSON, parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, 
				illegalFeatureName, illegalFeatureId, season.getEntitlementsRoot(), overrideIds,copiedStrings, destinationBranchId, season, context);

		if (illegalFeatureMinApp.size()>0 || illegalFeatureName.size()>0 || illegalFeatureId.size()>0 || errorFound) {
			JSONArray illegalFeatureMinAppArray = new JSONArray();
			JSONArray illegalFeatureNameArray = new JSONArray();
			JSONArray illegalFeatureIdArray = new JSONArray();

			for (int i=0; i<illegalFeatureMinApp.size(); i++)
				illegalFeatureMinAppArray.add(illegalFeatureMinApp.get(i));

			for (int i=0; i<illegalFeatureName.size(); i++)
				illegalFeatureNameArray.add(illegalFeatureName.get(i));

			for (int i=0; i<illegalFeatureId.size(); i++)
				illegalFeatureIdArray.add(illegalFeatureId.get(i));


			resultsJson.put(Constants.JSON_FIELD_ILLEGAL_MIN_APP_VER, illegalFeatureMinAppArray);
			resultsJson.put(Constants.JSON_FIELD_ILLEGAL_NAME, illegalFeatureName);
			resultsJson.put(Constants.JSON_FIELD_ILLEGAL_ID, illegalFeatureId);
			errorFound = true;
		}

		if (errorFound)
			return resultsJson;

		return null;
	}

	private static void doGetNameAndMinAppVerAndIDsViolationsFromJSON(Map<String, BaseAirlockItem> airlockItemsDB,
			JSONObject featureJSON, String parentSeasonMaxVer, String nameSuffix, 
			LinkedList<JSONObject> illegalFeatureMinApp, LinkedList<JSONObject> illegalFeatureName, LinkedList<JSONObject> illegalFeatureId,
			BaseAirlockItem root, boolean overrideIds,List<OriginalString> copiedStrings, String destinationBranchId,
			Season season, ServletContext context) throws JSONException {

		String typeStr = featureJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE);  
		Type typeObj = BaseAirlockItem.strToType(typeStr);
		if (typeObj == null)
			throw new JSONException(Strings.illegalType + typeStr);

		String id = featureJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);

		if (!overrideIds) {
			//verify that the Id is unique in masters and all existing branches
			if (isIdInUse(id, context)) {
				JSONObject illegalFeatureJSON = new  JSONObject();				
				illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, id);
				illegalFeatureId.add(illegalFeatureJSON);
			}				
		}

		if (typeObj == Type.FEATURE || typeObj == Type.CONFIGURATION_RULE || typeObj == Type.ENTITLEMENT || typeObj == Type.PURCHASE_OPTIONS) { 
			String minAppVer = featureJSON.getString(Constants.JSON_FEATURE_FIELD_MIN_APP_VER);
			String name = featureJSON.getString(Constants.JSON_FIELD_NAME); 
			String namespace = featureJSON.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE); 


			if (parentSeasonMaxVer!=null && (Season.compare(minAppVer, parentSeasonMaxVer)>=0)) {  
				JSONObject illegalFeatureJSON = new  JSONObject();
				illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, namespace+"."+name);
				illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, id);
				illegalFeatureMinApp.add(illegalFeatureJSON);
			}

			if (DataAirlockItem.verifyNamespaceNameUniqueness(namespace, name + nameSuffix, root, null, typeObj) != null) {
				JSONObject illegalFeatureJSON = new  JSONObject();
				illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, namespace+"."+name+nameSuffix);
				illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, id);
				illegalFeatureName.add(illegalFeatureJSON);
			}				
			else {
				String namespaceDotName = namespace + "."+ name + nameSuffix;

				if (destinationBranchId!=null && destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
					//when copying to master we should verify that this feature does not exists in one of it branches
					boolean found = false;
					for (Branch b:season.getBranches().getBranchesList()) {
						Set<String> branchesFeatures = b.getBranchAirlockItemsBD().keySet();
						for (String branchFeatureId:branchesFeatures) {				
							BaseAirlockItem branchAI = b.getBranchAirlockItemsBD().get(branchFeatureId);
							if (branchAI.getBranchStatus().equals(BranchStatus.NEW) && branchAI instanceof DataAirlockItem) {
								String nsDotName = ((DataAirlockItem)(branchAI)).getNameSpaceDotName();
								if (namespaceDotName.equals(nsDotName)) {
									JSONObject illegalFeatureJSON = new  JSONObject();
									illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, namespaceDotName);
									illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, id);
									illegalFeatureName.add(illegalFeatureJSON);
									found=true;
									break;
								}
							}
							if (found) {
								break;
							}
						}
					}
				}
				else { // if importing to branch - verify name uniqueness within branch
					if (destinationBranchId!=null && !destinationBranchId.equals(Constants.MASTER_BRANCH_NAME)) {
						//verify that name is unique in branch
						@SuppressWarnings("unchecked")
						Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

						Branch branch = branchesDB.get(destinationBranchId);
						if (branch.getBranchFeatureByName(namespaceDotName)!=null) {
							JSONObject illegalFeatureJSON = new  JSONObject();
							illegalFeatureJSON.put(Constants.JSON_FIELD_NAME, namespaceDotName);
							illegalFeatureJSON.put(Constants.JSON_FIELD_UNIQUE_ID, id);
							illegalFeatureName.add(illegalFeatureJSON);

						}
					}
				}
			}
		}

		JSONArray subFeatures = null;
		try {
			if (featureJSON.has(Constants.JSON_FEATURE_FIELD_FEATURES)) {
				subFeatures= featureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
			}
		} catch (Exception e) {

		}

		if (subFeatures!=null) {
			for (int i=0; i<subFeatures.size(); i++) {
				doGetNameAndMinAppVerAndIDsViolationsFromJSON(airlockItemsDB, (subFeatures.getJSONObject(i)),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, illegalFeatureId, root, overrideIds,copiedStrings, destinationBranchId, season, context);
			}
		}

		JSONArray subConfigRules = null;
		try {
			if (featureJSON.has(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)) {
				subConfigRules= featureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
			}
		} catch (Exception e) {

		}

		if (subConfigRules!=null) {
			for (int i=0; i<subConfigRules.size(); i++) {
				doGetNameAndMinAppVerAndIDsViolationsFromJSON(airlockItemsDB, (subConfigRules.getJSONObject(i)),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName,illegalFeatureId,  root, overrideIds,copiedStrings, destinationBranchId, season, context);
			}
		}	
		
		JSONArray subEntitlements = null;
		try {
			if (featureJSON.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {
				subEntitlements= featureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
			}
		} catch (Exception e) {
		}

		if (subEntitlements!=null) {
			for (int i=0; i<subEntitlements.size(); i++) {
				doGetNameAndMinAppVerAndIDsViolationsFromJSON(airlockItemsDB, (subEntitlements.getJSONObject(i)),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, illegalFeatureId, root, overrideIds,copiedStrings, destinationBranchId, season, context);
			}
		}

		JSONArray subPurchaseOptions = null;
		try {
			if (featureJSON.has(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)) {
				subPurchaseOptions= featureJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);
			}
		} catch (Exception e) {
		}

		if (subPurchaseOptions!=null) {
			for (int i=0; i<subPurchaseOptions.size(); i++) {
				doGetNameAndMinAppVerAndIDsViolationsFromJSON(airlockItemsDB, (subPurchaseOptions.getJSONObject(i)),
						parentSeasonMaxVer, nameSuffix, illegalFeatureMinApp, illegalFeatureName, illegalFeatureId, root, overrideIds,copiedStrings, destinationBranchId, season, context);
			}
		}

	}

	private static boolean isIdInUse(String id, ServletContext context) {
		@SuppressWarnings("unchecked")
		Map<String, BaseAirlockItem> mastersAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);
		if(mastersAirlockItemsDB.get(id)!=null)
			return true;

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Set<String> products = productsDB.keySet();
		for (String prodId:products) {
			Product prod = productsDB.get(prodId);
			for (Season season:prod.getSeasons()) {
				for (Branch branch:season.getBranches().getBranchesList()) {
					if (branch.getBranchAirlockItemsBD().get(id)!=null)
						return true;
				}
			}
		}

		return false;
	}

	public static ValidationResults copyFromJSON(Map<String, BaseAirlockItem> airlockItemsMapCopy, JSONObject featureToCopyJSON, String newParentId, String nameSuffix, String minAppVersion,
			InternalUserGroups userGroups, UserInfo userInfo, ServletContext context, boolean overrideId, Map<String, String> oldToNewIDsMap, Environment env, List<OriginalString> copiedStrings, Branch destinationBranchCopy, 
			Map<String, BaseAirlockItem> destinationAirlockItemsDB, REQUEST_ITEM_TYPE itemType) throws JSONException {

		ValidationCache tester = new ValidationCache(null, copiedStrings);
		
		return doCopyFromJSON(airlockItemsMapCopy, featureToCopyJSON, newParentId, nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env,copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
	}

	public static ValidationResults doCopyFromJSON(Map<String, BaseAirlockItem> airlockItemsMapCopy, JSONObject featureToCopyJSON, String newParentId, String nameSuffix, String minAppVersion,
			InternalUserGroups userGroups, UserInfo userInfo, ServletContext context, boolean overrideId, Map<String, String> oldToNewIDsMap, Environment env,List<OriginalString> copiedStrings,
			ValidationCache tester,
			Branch destinationBranchCopy, REQUEST_ITEM_TYPE itemType, Map<String, BaseAirlockItem> destinationAirlockItemsDB) {
		try {
			//go over all the features to import validate and add one by one (without children) using the copy map.
			BaseAirlockItem parentFeature = airlockItemsMapCopy.get(newParentId);

			String typeStr = featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_TYPE);  
			Type typeObj = BaseAirlockItem.strToType(typeStr);
			if (typeObj == null)
				throw new JSONException(Strings.illegalType + typeStr);


			JSONArray subFeatures = null;
			try {
				if (featureToCopyJSON.has(Constants.JSON_FEATURE_FIELD_FEATURES)) {
					subFeatures= featureToCopyJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
				}
			} catch (Exception e) {}

			JSONArray subConfigRules = null;
			try {
				if (featureToCopyJSON.has(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES)) {					
					subConfigRules= featureToCopyJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
				}
			} catch (Exception e) {}


			JSONArray subOrderingRules = null;
			try {
				if (featureToCopyJSON.has(Constants.JSON_FEATURE_FIELD_ORDERING_RULES)) {					
					subOrderingRules= featureToCopyJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
				}
			} catch (Exception e) {}

			JSONArray subEntitlements = null;
			try {
				if (featureToCopyJSON.has(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS)) {					
					subEntitlements= featureToCopyJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);
				}
			} catch (Exception e) {}

			JSONArray subPurchaseOptions = null;
			try {
				if (featureToCopyJSON.has(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS)) {					
					subPurchaseOptions= featureToCopyJSON.getJSONArray(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);
				}
			} catch (Exception e) {}


			String id = featureToCopyJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);

			featureToCopyJSON.remove(Constants.JSON_FIELD_UNIQUE_ID);			
			featureToCopyJSON.remove(Constants.JSON_FIELD_LAST_MODIFIED);
			featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_STATUS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_STATUS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_FEATURES_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_FEATURE_PARENT_NAME);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS);
			featureToCopyJSON.remove(Constants.JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS);

			featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_SEASON_ID, parentFeature.getSeasonId().toString());
			if (typeObj == Type.FEATURE || typeObj == Type.CONFIGURATION_RULE || typeObj == Type.ORDERING_RULE || typeObj == Type.PURCHASE_OPTIONS || typeObj == Type.ENTITLEMENT) { 
				String name = featureToCopyJSON.getString(Constants.JSON_FIELD_NAME); 

				featureToCopyJSON.put(Constants.JSON_FIELD_NAME, name+nameSuffix);
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_STAGE, Stage.DEVELOPMENT.toString()); //move copied features to dev
			}

			if (subFeatures != null) 
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_FEATURES);

			if (subConfigRules!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);

			if (subOrderingRules!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);

			if (subEntitlements!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_ENTITLEMENTS);

			if (subPurchaseOptions!=null)
				featureToCopyJSON.remove(Constants.JSON_FEATURE_FIELD_PURCHASE_OPTIONS);

			if (minAppVersion!=null)
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_MIN_APP_VER, minAppVersion);

			if (typeObj.equals(Type.ORDERING_RULE)) {
				String configuration = featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_CONFIGURATION);
				Stage stage = Utilities.strToStage(featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE));
				Rule rule = new Rule();
				rule.fromJSON(featureToCopyJSON.getJSONObject(Constants.JSON_FEATURE_FIELD_RULE));

				//fix the configuration - replace the subFeatures ids with the new ids
				String newConfig = OrderingRuleItem.replaceIdsInConfiguration(tester, context, oldToNewIDsMap, configuration, rule, parentFeature.getSeasonId().toString(), stage, minAppVersion);
				featureToCopyJSON.put(Constants.JSON_FEATURE_FIELD_CONFIGURATION, newConfig);
			}

			BaseAirlockItem newAirlockObj = BaseAirlockItem.getAirlockItemByType(featureToCopyJSON);
			if (newAirlockObj == null) {
				//should never happen!
				String errMsg = Strings.typeNotFound;
				logger.severe(errMsg);
				return null;
			}

			LinkedList<String> addedSubFeatures = new LinkedList<String>();
			LinkedList<String> missingSubFeatures = new LinkedList<String>();

			//check that each featureId appears only once in the update tree
			HashMap<UUID, Integer> existingFeaturesInUpdate = new HashMap<UUID, Integer>();  

			//if is premium feature - verify that entitlement exists in the destination
			if (typeObj.equals(Type.FEATURE) && featureToCopyJSON.containsKey(Constants.JSON_FIELD_ENTITLEMENT) && featureToCopyJSON.get(Constants.JSON_FIELD_ENTITLEMENT)!=null) {
				String entitlementId = featureToCopyJSON.getString(Constants.JSON_FIELD_ENTITLEMENT);
				if (entitlementId!=null && !entitlementId.isEmpty()) {
					EntitlementItem entitlementInSource = (EntitlementItem)destinationAirlockItemsDB.get(entitlementId);
					String entitlementName = null;
					if (entitlementInSource == null) {
						if (featureToCopyJSON.containsKey(Constants.JSON_FIELD_ENTITLEMENT_NAME) && featureToCopyJSON.get(Constants.JSON_FIELD_ENTITLEMENT_NAME)!=null && !featureToCopyJSON.getString(Constants.JSON_FIELD_ENTITLEMENT_NAME).isEmpty()) {
						 	entitlementName = featureToCopyJSON.getString(Constants.JSON_FIELD_ENTITLEMENT_NAME);
						} 
					}
					else {
						entitlementName = entitlementInSource.getNameSpaceDotName();
					}

					if (entitlementName == null) {
						return new ValidationResults(String.format(Strings.cannotImportMissingEntitlement, entitlementId, featureToCopyJSON.getString(Constants.JSON_FIELD_NAME)), Status.BAD_REQUEST);
					}
					
					EntitlementItem entitlementInDestination = Utilities.getEntitlementByNameStageAndSeason(airlockItemsMapCopy, entitlementName, parentFeature.getSeasonId(), Stage.valueOf(featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE)));
					if (entitlementInDestination == null) {
						return new ValidationResults(String.format(Strings.cannotImportMissingEntitlement, entitlementName, featureToCopyJSON.getString(Constants.JSON_FIELD_NAME)), Status.BAD_REQUEST);
					}
					featureToCopyJSON.put(Constants.JSON_FIELD_ENTITLEMENT, entitlementInDestination.getUniqueId().toString());
				}
			}
			
			//if is bundle - verify that includedEntitlements exists in the destination
			if (typeObj.equals(Type.ENTITLEMENT) && featureToCopyJSON.containsKey(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS) && featureToCopyJSON.get(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS)!=null) {
				JSONArray includedEntitlementsArr = featureToCopyJSON.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS);
				JSONArray includedEntitlementNamesArr = null;
				if (featureToCopyJSON.containsKey(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS_NAMES) && featureToCopyJSON.get(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS_NAMES)!=null) {
					includedEntitlementNamesArr = featureToCopyJSON.getJSONArray(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS_NAMES);
				}
				JSONArray newIncludedEntitlementsArr = new JSONArray();
				for (int j =0; j<includedEntitlementsArr.size(); j++) {
					String entitlementId = includedEntitlementsArr.getString(j);
					if (entitlementId!=null && !entitlementId.isEmpty()) {
						EntitlementItem entitlementInSource = (EntitlementItem)destinationAirlockItemsDB.get(entitlementId);
						String entitlementInSourceName = "";
						if (entitlementInSource == null && includedEntitlementNamesArr!=null) {
							entitlementInSourceName = includedEntitlementNamesArr.getString(j);
						}
						else {
							entitlementInSourceName = entitlementInSource.getNameSpaceDotName();
						}
						
						if (entitlementInSourceName == null) {
							return new ValidationResults(String.format(Strings.cannotImportMissingIncludedEntitlement, entitlementId, featureToCopyJSON.getString(Constants.JSON_FIELD_NAME)), Status.BAD_REQUEST);
						}
						
						EntitlementItem entitlementInDestination = Utilities.getEntitlementByNameStageAndSeason(airlockItemsMapCopy, entitlementInSourceName, parentFeature.getSeasonId(), Stage.valueOf(featureToCopyJSON.getString(Constants.JSON_FEATURE_FIELD_STAGE)));
						if (entitlementInDestination == null) {
							return new ValidationResults(String.format(Strings.cannotImportMissingIncludedEntitlement, entitlementInSourceName, featureToCopyJSON.getString(Constants.JSON_FIELD_NAME)), Status.BAD_REQUEST);
						}
						newIncludedEntitlementsArr.add(entitlementInDestination.getUniqueId().toString());
					}
				}
				featureToCopyJSON.put(Constants.JSON_FIELD_INCLUDED_ENTITLEMENTS, newIncludedEntitlementsArr);
			}
			HashMap<String, JSONObject> updatedFeatures = new HashMap<String, JSONObject>();

			ValidationResults validationRes = newAirlockObj.doValidateFeatureJSON(featureToCopyJSON, context, parentFeature.getSeasonId().toString(), addedSubFeatures, missingSubFeatures, userGroups, existingFeaturesInUpdate, newParentId, updatedFeatures, userInfo, tester, airlockItemsMapCopy, env,copiedStrings);
			if (validationRes!=null) 
				return validationRes;


			newAirlockObj.fromJSON(featureToCopyJSON, airlockItemsMapCopy, UUID.fromString(newParentId), env);
			if (overrideId)
				newAirlockObj.setUniqueId(UUID.randomUUID());
			else 
				newAirlockObj.setUniqueId(UUID.fromString(id));

			oldToNewIDsMap.put(id, newAirlockObj.getUniqueId().toString());

			if (!env.isInMaster()) {
				//add feature to branch.								
				String err = destinationBranchCopy.addAirlockItem (newAirlockObj, parentFeature, itemType);
				//err = parentFeature.addAirlockItem(newAirlockObj);
				parentFeature.setLastModified(new Date());
				if (err!=null) {
					logger.severe(err);
					return new ValidationResults(err, Status.BAD_REQUEST);
				}
				airlockItemsMapCopy.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
				newAirlockObj.setBranchStatus(BranchStatus.NEW);
			}
			else {
				String err = parentFeature.addAirlockItem(newAirlockObj);
				if (err!=null) {
					logger.severe(err);
					return new ValidationResults (err, Status.BAD_REQUEST);
				}
				parentFeature.setLastModified(new Date());

				airlockItemsMapCopy.put(newAirlockObj.getUniqueId().toString(), newAirlockObj);
			}

			if (subConfigRules!=null) {
				for (int i=0; i<subConfigRules.size(); i++) {
					JSONObject subFeatureToCopy = subConfigRules.getJSONObject(i);			
					validationRes = doCopyFromJSON(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env, copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
					if (validationRes!=null) 
						return validationRes;				
				}
			}

			if (subFeatures!=null) {
				for (int i=0; i<subFeatures.size(); i++) {
					JSONObject subFeatureToCopy = subFeatures.getJSONObject(i);			
					validationRes = doCopyFromJSON(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env, copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
					if (validationRes!=null) 
						return validationRes;

				}
			}

			if (subOrderingRules!=null) {
				for (int i=0; i<subOrderingRules.size(); i++) {
					JSONObject subFeatureToCopy = subOrderingRules.getJSONObject(i);			
					validationRes = doCopyFromJSON(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env, copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
					if (validationRes!=null) 
						return validationRes;

				}
			}
			
			if (subEntitlements!=null) {
				for (int i=0; i<subEntitlements.size(); i++) {
					JSONObject subFeatureToCopy = subEntitlements.getJSONObject(i);			
					validationRes = doCopyFromJSON(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env, copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
					if (validationRes!=null) 
						return validationRes;

				}
			}
			
			if (subPurchaseOptions!=null) {
				for (int i=0; i<subPurchaseOptions.size(); i++) {
					JSONObject subFeatureToCopy = subPurchaseOptions.getJSONObject(i);			
					validationRes = doCopyFromJSON(airlockItemsMapCopy, subFeatureToCopy, newAirlockObj.getUniqueId().toString(), nameSuffix, minAppVersion, userGroups, userInfo, context, overrideId, oldToNewIDsMap, env, copiedStrings, tester, destinationBranchCopy, itemType, destinationAirlockItemsDB);
					if (validationRes!=null) 
						return validationRes;
				}
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);			
		}
		return null;
	}

	public static boolean stringArrayCompareIgnoreOrder(JSONArray jsonArr, String[] strArr) throws JSONException {
		return compareIgnoreOrder(jsonArr, strArr == null? null: Arrays.asList( strArr ));			
	}

	public static boolean compareIgnoreOrder(JSONArray jsonArr, List<String> strList) throws JSONException {

		if ((jsonArr == null || jsonArr.isEmpty()) && ((strList == null) || strList.size() == 0))
			return true; //both null or empty

		if (jsonArr == null || strList == null)
			return false; //only one is null and the other is not empty

		HashSet<String> h1 = new HashSet<String>( Arrays.asList( Utilities.jsonArrToStringArr(jsonArr) ));
		HashSet<String> h2 = new HashSet<String>(strList);
		return h1.equals(h2);		
	}			
	public static boolean compareListsIgnoreOrder(List<String> list1, List<String> list2) throws JSONException {

		if (list1 == null && list2 == null)
			return true; //both null

		if (list1 == null || list2 == null)
			return false; //only one is null and the other is not empty

		HashSet<String> h1 = new HashSet<String>(list1);
		HashSet<String> h2 = new HashSet<String>(list2);
		return h1.equals(h2);		
	}			


	public static List<String> convertRolesListToStringList(LinkedList<Constants.RoleType> rolesList) {
		List<String> rolesStrList = null;
		if (rolesList != null) {
			rolesStrList = new LinkedList<String>();
			for (RoleType rt:rolesList) {
				rolesStrList.add(rt.toString());		
			}
		}
		return rolesStrList;
	}

	public static boolean compareRolesIgnoreOrder(JSONArray rolesJsonArr, LinkedList<Constants.RoleType> rolesList) throws JSONException {
		List<String> rolesStrList = convertRolesListToStringList(rolesList);

		if ((rolesJsonArr == null || rolesJsonArr.isEmpty()) && ((rolesList == null) || rolesList.size() == 0))
			return true; //both null or empty

		if (rolesJsonArr == null || rolesList == null)
			return false; //only one is null and the other is not empty


		HashSet<String> h1 = new HashSet<String>( Arrays.asList( Utilities.jsonArrToStringArr(rolesJsonArr) ));
		HashSet<String> h2 = new HashSet<String>(rolesStrList);
		return h1.equals(h2);
	}

	public static boolean compareStringsArrayToStringsList(JSONArray jsonArr, List<String> strList) throws JSONException {

		if ((jsonArr == null || jsonArr.isEmpty()) && ((strList == null) || strList.size() == 0))
			return true; //both null or empty

		if (jsonArr == null || strList == null)
			return false; //only one is null and the other is not empty

		if (jsonArr.size()!=strList.size())
			return false; //different lists sizes

		for (int i=0; i<jsonArr.size(); i++) {
			if (!jsonArr.getString(i).equals(strList.get(i)))
				return false;
		}

		return true;
	}

	// flatten: get a flat list of JSON property keys (embedded objects are separated by periods.  array items show as [n] )

	// arraySpec contains array specifications: which items of an array should be displayed.
	// if arraySpec == null, show all items of all arrays
	// if initialized, but a specification for current array is not found, show the first item of the array.
	// else, show specific items for the array. Example format: { "some.path.to.array" : "1,3,5-7,10" }

	public static boolean flatten(Object obj, String parent, TreeSet<String> attrMap, TreeMap<String,String> arraySpec) throws JSONException
	{
		if (obj instanceof JSONObject)
		{
			String prefix = (parent == null) ? "" : parent + ".";
			JSONObject attr = (JSONObject) obj;
			@SuppressWarnings("unchecked")
			Set<String> keys = attr.keySet();
			for (String str : keys)
			{
				String full = prefix + str;
				boolean isLeaf = flatten(attr.get(str), full, attrMap, arraySpec);
				if (isLeaf)
					attrMap.add(full);
			}
			return false;
		}
		if (obj instanceof JSONArray)
		{
			String prefix = (parent == null) ? "[" : parent + "[";
			JSONArray arr = (JSONArray) obj;
			int[] items = getArrayIndices(arr, parent, arraySpec);
			for (int i = 0; i < items.length; ++i)
			{
				String full = prefix + items[i] + "]";
				boolean isLeaf = flatten(arr.get(items[i]), full, attrMap, arraySpec);
				if (isLeaf)
					attrMap.add(full);
			}
			return false;
		}
		// else ignore
		return true; // is leaf
	}

	private static int[] getArrayIndices(JSONArray arr, String parent, TreeMap<String,String> arraySpec)
	{
		String spec = (arraySpec == null) ? null : arraySpec.get(parent);

		int len = -1;
		if (arraySpec == null)
			len = arr.length(); // show all items
		else if (spec == null)
			len = arr.length() > 0 ? 1 : 0; // no specification for current array; show first item

			if (len != -1)
			{
				int[] out = new int[len];
				for (int i = 0; i < len; ++i)
					out[i] = i;
				return out;
			}

			TreeSet<Integer> selection = new TreeSet<Integer>();
			for (String item : spec.split("\\s*,\\s*"))
			{
				String[] spair = item.split("\\s*-\\s*");
				int[] pair = parsePair(spair);
				if (pair == null)
					continue; // throw error?

				for (int i = pair[0]; i <= pair[1]; ++i)
				{
					if (i < arr.length())
						selection.add(i);
				}
			}
			int[] out = new int[selection.size()];
			int count = 0;
			for (Integer i : selection)
			{
				out[count++] = i;
			}
			return out;
	}
	private static int[] parsePair(String[] pair)
	{
		if (pair.length != 1 && pair.length != 2)
			return null;

		try {
			int[] out = new int[2];
			for (int i = 0; i < pair.length; ++i)
			{
				out[i] = Integer.parseInt(pair[i]);
				if (out[i] < 0)
					throw new Exception();
			}

			if (pair.length == 1)
				out[1] = out[0];
			return out;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static Map<String, String> getStageForInputFields(ServletContext context, JSONObject inputSchema) throws GenerationException, JSONException {
		String generator = (String)context.getAttribute(Constants.SCHEMA_JSON_GENERATOR_PARAM_NAME);				
		String prune = (String)context.getAttribute(Constants.SCHEMA_JSON_PRUNE_PARAM_NAME);
		String jsf = (String)context.getAttribute(Constants.SCHEMA_JSON_FAKER_PARAM_NAME);

		return JsonGenerator.getStageForAllFields(generator, jsf, prune, inputSchema.toString());
	}

	public static RootItem findItemsRoot(BaseAirlockItem airlockItem, Map<String, BaseAirlockItem> airlockItemsMapCopy) {
		if (airlockItem.getType() == Type.ROOT)
			return (RootItem)airlockItem;

		while (airlockItem.getType() != Type.ROOT) {
			UUID parentId = airlockItem.getParent();
			BaseAirlockItem parent = null;
			if (parentId == null) {
				//this can be in branch in the root of a subTree. the parentId is null but the branchParentName exists
				parent = getFeatureByName(airlockItem.getBranchFeatureParentName(), airlockItemsMapCopy);
			}
			else {
				parent = airlockItemsMapCopy.get(parentId.toString());
			}
			airlockItem = parent;
		}

		return (RootItem)airlockItem;
	}

	public static BaseAirlockItem getFeatureByName(String branchFeatureParentName, Map<String, BaseAirlockItem> airlockItemsMap) {
		if (branchFeatureParentName == null) 
			return null;

		Set<String> featureIds = airlockItemsMap.keySet();
		for (String featureId:featureIds) {
			BaseAirlockItem alItem = airlockItemsMap.get(featureId);
			if (Branch.getItemBranchName(alItem).equals(branchFeatureParentName))
				return alItem;
		}

		return null;		
	}

	//return null if no such stage	
	public static TranslationStatus strToTranslationStatus(String translationStatusStr) {
		return valueOf(TranslationStatus.class, translationStatusStr);
	}

	//return null if no such stage	
	public static StringStatus strToStringStatus(String stringStatusStr) {
		if (stringStatusStr == null) 
			return null;

		if(stringStatusStr.equalsIgnoreCase(StringStatus.NEW_STRING.toString())) 
			return StringStatus.NEW_STRING;

		if(stringStatusStr.equalsIgnoreCase(StringStatus.READY_FOR_TRANSLATION.toString())) 
			return StringStatus.READY_FOR_TRANSLATION;		

		if(stringStatusStr.equalsIgnoreCase(StringStatus.REVIEWED_FOR_TRANSLATION.toString())) 
			return StringStatus.REVIEWED_FOR_TRANSLATION;		

		if(stringStatusStr.equalsIgnoreCase(StringStatus.IN_TRANSLATION.toString())) 
			return StringStatus.IN_TRANSLATION;

		if(stringStatusStr.equalsIgnoreCase(StringStatus.TRANSLATION_COMPLETE.toString())) 
			return StringStatus.TRANSLATION_COMPLETE;

		return null; 
	}

	public static GetAnalyticsOutputMode strToGetAnalyticsOutputMode(String modeStr) {
		if (modeStr == null) 
			return null;

		if(modeStr.equalsIgnoreCase(GetAnalyticsOutputMode.BASIC.toString())) 
			return GetAnalyticsOutputMode.BASIC;


		if(modeStr.equalsIgnoreCase(GetAnalyticsOutputMode.VERBOSE.toString())) 
			return GetAnalyticsOutputMode.VERBOSE;

		if(modeStr.equalsIgnoreCase(GetAnalyticsOutputMode.DISPLAY.toString())) 
			return GetAnalyticsOutputMode.DISPLAY;

		return null; 
	}

	public static AttributeType strToAttributeType(String attTypeStr) {
		return valueOf(AttributeType.class, attTypeStr);
	}

	public static UtilityType strToUtilityType(String utilTypeStr) {
		return valueOf(UtilityType.class, utilTypeStr);
	}

	// this wrapper returns null on error instead of exception
	public static <E extends Enum<E>> E valueOf(Class<E> clazz, String name)
	{
		try {
			return Enum.valueOf(clazz, name);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static RoleType strToRoleType(String roleTypeStr)
	{
		return valueOf(RoleType.class, roleTypeStr);
	}

	public static AirlockCapability strToAirlockCapability(String capabilityStr)
	{
		return valueOf(AirlockCapability.class, capabilityStr);
	}

	public static ValidationResults validatePercentage(Double p) {
		if (p<0 || p>100) {  
			return new ValidationResults("rolloutPercentage should be a number between 0-100.", Status.BAD_REQUEST);
		}

		if (p == 0.0001 || p == 0.0002 || p == 0.0003 || p == 0.0004 || p == 0.0005 || p == 0.0006 || p == 0.0007 || p == 0.0008 || p == 0.0009)
			return null;


		String pStr = p.toString();
		//String pStr = String.format(Locale.US, "%f", p) ;
		if (p.toString().contains("e") || p.toString().contains("E")) //scientific notation of number < 0.0001
			return new ValidationResults("The Rollout Percentage must be a number between 0-100. The number can have up to 4 decimal places, for example: 37.4923.", Status.BAD_REQUEST);

		if (!p.toString().contains(".") && !p.toString().contains(",")) //the comma is for French locale double 
			return null; //int between 0 to 100

		String[] parts = pStr.split("[\\.\\,]"); //only 2 parts since legal double 
		if (parts[1].length() > 4)
			return new ValidationResults("The Rollout Percentage must be a number between 0-100. The number can have up to 4 decimal places, for example: 37.4923.", Status.BAD_REQUEST);

		return null;
		/*
		long round = Math.round(p * 10000);
		double test =  round / 10000.0;
		double diff = p - test;
		//boolean error = Math.abs(diff) > 0.00001;

		if (Math.abs(diff) > 0.00001) {
			return new ValidationResults("The Rollout Percentage must be a number between 0-100. The number can have up to 4 decimal places, for example: 37.4923.", Status.BAD_REQUEST);
		}*/

	}

	public static List<ChangeDetails> sanitizeChangesList(List<ChangeDetails> changeDetailsList){
		if(changeDetailsList.isEmpty()) {
			return changeDetailsList;
		}
		HashMap<UUID,ChangeDetails>  changeMap =  new HashMap<>();
		for (int i = 0; i<changeDetailsList.size();++i) {
			ChangeDetails change = changeDetailsList.get(i);
			ChangeDetails inMapChange = changeMap.get(change.getUniqueId());
			if(inMapChange == null){
				changeMap.put(change.getUniqueId(),change);
			}
			else {
				inMapChange.addUpdateDetails(change.getUpdateDetails());
			}
		}
		return new ArrayList<>(changeMap.values());
	}

	public static String getConsoleString(ServletContext context){
		String consoleUrl = (String)context.getAttribute(Constants.CONSOLE_URL);
		String consoleString = "";
		if(consoleUrl != null && !consoleUrl.isEmpty()){
			consoleString = "Link to Airlock Control Center: "+consoleUrl + "<br/>";
		}
		return consoleString;
	}

	public static String getServerName(ServletContext context){
		AirlockServers allServers = (AirlockServers)context.getAttribute(Constants.AIRLOCK_SERVERS_PARAM_NAME);
		String serverName = allServers.getDefaultServer();
		if(serverName.equals(Constants.SERVER_DEFAULT_DISPLAY_NAME)){
			serverName = getEnv(Constants.ENV_SERVER_NAME);
		}
		return serverName;
	}

	public static void sendEmails(ServletContext context,String rootCause,UUID apiCallID,List<ChangeDetails> changeDetailsList,UserInfo userInfo,Map<String, BaseAirlockItem> airlockItemsDB,Environment env){
		for (int i = 0; i<changeDetailsList.size();++i){
			ChangeDetails change = changeDetailsList.get(i);
			if(change.getItem() instanceof DataAirlockItem) {
				sendEmailForDataItem(context, change.getFirstParentFeature(airlockItemsDB), change.getFollowers(context,airlockItemsDB), change.getUpdateDetails(),rootCause,apiCallID, change.isProduction(), userInfo,env);
			}
		}
	}

	public static void sendEmailForProduct(ServletContext context, UserInfo userInfo,Product product){
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> productsFollowersDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		ArrayList<String> followers = productsFollowersDB.get(product.getUniqueId().toString());
		if(followers == null || followers.size() == 0) {
			return;
		}

		String serverName = getServerName(context);
		String subject = "[Airlock]" + separator +" Product " + product.getName() + " was deleted"+ separator + "("+ serverName+ ")";

		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
		Date now = new Date();
		String changeDate = sdfDate.format(now);
		String consoleString = getConsoleString(context);
		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", followers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("details","The product was deleted");
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String mailBody = "<strong>Update: <br/>" +
					"The product "+ product.getName() + " was deleted"+ "</strong><br/><br/>"+
					"<strong> Details: </strong><br/>"+
					"User:<strong> "+ userId +"</strong><br/>"+
					"Date:<strong> " + changeDate + "</strong><br/>"+
					"Product:<strong> " + product.getName()+ "</strong><br/><br/>"+
					"<strong>Additional info:</strong> <br/>"+
					consoleString +
					"Server:<strong> "+ serverName +"</strong><br/>"+
					"ProductID:<strong> " + product.getUniqueId()+ "</strong><br/>";
			sendEmail(context, followers, subject, mailBody);
		}
	}

	public static String getSeasonString(Season season) {
		if (season == null) {
			return null;
		}
		StringBuilder versionRange = new StringBuilder(""+season.getMinVersion());
		if(season.getMaxVersion() != null) {
			versionRange.append(" to " + season.getMaxVersion() +"");
		}
		else {
			versionRange.append(" and up");
		}
		return versionRange.toString();
	}
	public static void sendEmailForSeason(ServletContext context, UserInfo userInfo,Season season,String action){
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(season.getProductId().toString());
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> productsFollowersDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		ArrayList<String> followers = productsFollowersDB.get(season.getProductId().toString());
		if(followers == null || followers.size() == 0) {
			return;
		}

		StringBuilder versionRange = new StringBuilder(""+season.getMinVersion());
		if(season.getMaxVersion() != null) {
			versionRange.append(" to " + season.getMaxVersion() +"");
		}
		else {
			versionRange.append(" and up");
		}
		String serverName = getServerName(context);

		String subject = "[Airlock]"+ separator  +" Version Range " + versionRange + " " + action + " in product "+ product.getName()+ separator + "("+ serverName+ ")";

		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
		Date now = new Date();
		String changeDate = sdfDate.format(now);
		String consoleString = getConsoleString(context);

		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", followers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("seasonID",season.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("seasonMax",season.getMaxVersion());
				email.put("seasonMin",season.getMinVersion());
				email.put("details","The season was "+action);
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String mailBody = "<strong>Update: <br/>" +
					"A version range "+versionRange +" was "+ action+"  </strong><br/><br/>"+
					"<strong> Details: </strong><br/>"+
					"User:<strong> "+ userId +"</strong><br/>"+
					"Date:<strong> " + changeDate + "</strong><br/>"+
					"Product:<strong> " + product.getName()+ "</strong><br/>"+
					"Version range: <strong>"+ versionRange+ "</strong><br/><br/>"+
					"<strong>Additional info:</strong> <br/>"+
					consoleString +
					"Server:<strong> "+ serverName +"</strong><br/>"+
					"Product ID:<strong> " + product.getUniqueId()+ "</strong><br/>"+
					"Version range ID: <strong>"+ season.getUniqueId()+ "</strong><br/>";
			sendEmail(context, followers, subject, mailBody);
		}
	}

	public static void sendEmailForExperiment(ServletContext context, Experiment experiment,ArrayList<String> followers, String productId, String action,String details, Boolean productionChange, UserInfo userInfo) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersProductsDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);
		Product product = productsDB.get(productId);
		ArrayList<String> emailFollowers = new ArrayList<>();
		if(followers != null) {
			emailFollowers.addAll(followers);
		}
		//add product followers. duplicate don't matter
		ArrayList<String> productFollowers = null;
		if(productionChange == true){
			productFollowers = followersProductsDB.get(productId);
			if(productFollowers!=null) {
				emailFollowers.addAll(productFollowers);
			}
		}
		if(emailFollowers.size() == 0) {
			return;
		}
		String stage = "DEVELOPMENT";
		if(productionChange){
			stage = "PRODUCTION";
		}
		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		String serverName = getServerName(context);
		if(details == null){
			details = "The experiment was "+action;
		}
		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", emailFollowers);
				email.put("productFollowers", productFollowers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("experimentID",experiment.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("experiment",experiment.getName());
				email.put("details",details);
				email.put("stage",stage);
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String subject = "[Airlock]"+ separator  +" Experiment  " + experiment.getName() + " " + action + " in product "+ product.getName()+ separator + "("+ serverName+ ")";

			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
			Date now = new Date();
			String changeDate = sdfDate.format(now);

			String consoleString = getConsoleString(context);
			String mailBody =
					"<strong> Details: </strong><br/>"+
							"User:<strong> "+ userId +"</strong><br/>"+
							"Date:<strong> " + changeDate + "</strong><br/>"+
							"Product:<strong> " + product.getName()+ "</strong><br/>"+
							"Experiment:<strong> " + experiment.getName()+ "</strong><br/><br/>"+
							"<strong>Update: <br/>" +
							details.replace("\n","<br/>") +"</strong><br/><br/>"+

					"<strong>Additional info:</strong> <br/>"+
					consoleString +
					"Server:<strong> "+ serverName +"</strong><br/>"+
					"Product ID:<strong> " + product.getUniqueId()+ "</strong><br/>"+
					"Experiment ID:<strong> " + experiment.getUniqueId()+ "</strong><br/>";
			sendEmail(context, emailFollowers, subject, mailBody);
		}
	}

	public static void sendEmailForAllExperiments(ServletContext context, String productId,String details,UserInfo userInfo) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersProductsDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		Product product = productsDB.get(productId);
		ArrayList<String> emailFollowers = followersProductsDB.get(productId);
		if(emailFollowers==null || emailFollowers.size() == 0) {
			return;
		}
		String stage  = "PRODUCTION";
		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		String serverName = getServerName(context);
		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", emailFollowers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("details",details);
				email.put("stage",stage);
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String subject = "[Airlock]"+ separator  +" Experiments were updated";

			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
			Date now = new Date();
			String changeDate = sdfDate.format(now);

			String consoleString = getConsoleString(context);
			String mailBody =
					"<strong> Details: </strong><br/>"+
							"User:<strong> "+ userId +"</strong><br/>"+
							"Date:<strong> " + changeDate + "</strong><br/>"+
							"Product:<strong> " + product.getName()+ "</strong><br/><br/>"+
							"<strong>Update: <br/>" +
							details.replace("\n","<br/>") +"</strong><br/><br/>"+
							"<strong>Additional info:</strong> <br/>"+
							consoleString +
							"Server:<strong> "+ serverName +"</strong><br/>"+
							"Product ID:<strong> " + product.getUniqueId()+ "</strong><br/>";
			sendEmail(context, emailFollowers, subject, mailBody);
		}
	}

	public static void sendEmailForVariant(ServletContext context, Variant variant, ArrayList<String> followers, String productId, String details, Boolean productionChange, UserInfo userInfo) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersProductsDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		Product product = productsDB.get(productId);
		ArrayList<String> emailFollowers = new ArrayList<>();
		if(followers != null) {
			emailFollowers.addAll(followers);
		}
		//add product followers. duplicate don't matter
		ArrayList<String> productFollowers = null;
		if(productionChange == true){
			productFollowers = followersProductsDB.get(productId);
			if(productFollowers!=null) {
				emailFollowers.addAll(productFollowers);
			}
		}
		if(emailFollowers.size() == 0) {
			return;
		}
		String stage = "DEVELOPMENT";
		if(productionChange){
			stage = "PRODUCTION";
		}
		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		String serverName = getServerName(context);
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);
		Experiment experiment = experimentsDB.get(variant.getExperimentId().toString());
		String branchName = variant.getBranchName();
		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", emailFollowers);
				email.put("productFollowers", productFollowers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("experimentID",experiment.getUniqueId().toString());
				email.put("variantID",variant.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("experiment",experiment.getName());
				email.put("variant",variant.getName());
				email.put("branch",branchName);
				email.put("details",details);
				email.put("stage",stage);
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String subject = "[Airlock]" + separator + stage + " CHANGES" + separator + "\""+ variant.getName()+
					"\"" + separator + product.getName() + separator + separator + "("+ serverName+ ")";

			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
			Date now = new Date();
			String changeDate = sdfDate.format(now);

			String consoleString = getConsoleString(context);
			String mailBody =
					"<strong> Details: </strong><br/>"+
							"User:<strong> "+ userId +"</strong><br/>"+
							"Date:<strong> " + changeDate + "</strong><br/>"+
							"Product:<strong> " + product.getName()+ "</strong><br/>"+
							"Experiment:<strong> " + experiment.getName()+ "</strong><br/>"+
							"Variant:<strong> " + variant.getName()+ "</strong><br/>"+
							"Branch:<strong> " + branchName+ "</strong><br/><br/>"+
							"<strong>Update: <br/>" +
							details.replace("\n","<br/>") +"</strong><br/><br/>"+
							"<strong>Additional info:</strong> <br/>"+
							consoleString +
							"Server:<strong> "+ serverName +"</strong><br/>"+
							"Product ID:<strong> " + product.getUniqueId()+ "</strong><br/>"+
							"Experiment ID:<strong> " + experiment.getUniqueId()+ "</strong><br/>"+
							"Variant ID:<strong> " + variant.getUniqueId()+ "</strong><br/>";
			sendEmail(context, emailFollowers, subject, mailBody);
		}
	}

	public static void sendEmailForDataItem(ServletContext context, FeatureItem featureToUpdate,ArrayList<String> followers,String details,String rootCause,UUID apiCallID,Boolean productionChange,UserInfo userInfo,Environment env) {
		if (featureToUpdate == null)
			return;

		String branchName = Constants.MASTER_BRANCH_NAME;
		if(env != null && !env.getBranchId().equals(Constants.MASTER_BRANCH_NAME)){
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Branch branch = branchesDB.get(env.getBranchId());
			if(branch.isPartOfExperimentInProduction(context) == null){
				return;
			}
			branchName = branch.getName();
		}
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, ArrayList<String>> followersProductsDB = (Map<String, ArrayList<String>>)context.getAttribute(Constants.FOLLOWERS_PRODUCTS_DB_PARAM_NAME);

		Season season = seasonsDB.get(featureToUpdate.getSeasonId().toString());
		Product product = productsDB.get(season.getProductId().toString());
		ArrayList<String> emailFollowers = new ArrayList<>();
		if(followers != null && branchName.equals(Constants.MASTER_BRANCH_NAME)) {
			emailFollowers.addAll(followers);
		}
		//add product followers. duplicate don't matter
		ArrayList<String> productFollowers = null;
		if(productionChange == true){
			productFollowers = followersProductsDB.get(season.getProductId().toString());
			if(productFollowers!=null) {
				emailFollowers.addAll(productFollowers);
			}
		}
		if(emailFollowers.size() == 0) {
			return;
		}
		String minAppVersion = featureToUpdate.getMinAppVersion();
		String defaultIfDown = featureToUpdate.getDefaultIfAirlockSystemIsDown()? "On":"Off";
		String cachedResults = featureToUpdate.noCachedResults? "No":"Yes";

		StringBuilder versionRange = new StringBuilder(season.getMinVersion());
		if(season.getMaxVersion() != null) {
			versionRange.append(" to " + season.getMaxVersion() );
		}
		else {
			versionRange.append(" and up");
		}
		String stage = "DEVELOPMENT";
		if(productionChange){
			stage = "PRODUCTION";
		}
		String userId ="Unknow";
		if(userInfo != null)
			userId = userInfo.getId();
		String serverName = getServerName(context);
		if ((Boolean) context.getAttribute(Constants.IS_TEST_MODE)) {
			try {
				JSONObject email = new JSONObject();
				email.put("followers", emailFollowers);
				email.put("productFollowers", productFollowers);
				email.put("server",serverName);
				email.put("productID",product.getUniqueId().toString());
				email.put("seasonID",season.getUniqueId().toString());
				email.put("itemID",featureToUpdate.getUniqueId().toString());
				email.put("user",userId);
				email.put("product",product.getName());
				email.put("seasonMax",season.getMaxVersion());
				email.put("seasonMin",season.getMinVersion());
				email.put("item",featureToUpdate.getNameSpaceDotName());
				email.put("branch",branchName);
				email.put("details",details);
				email.put("stage",stage);
				email.put("minAppVersion", minAppVersion);
				email.put("defaultIfDown",defaultIfDown);
				email.put("noCachedResult",cachedResults);
				email.put("itemType",featureToUpdate.getType().toString());
				writeMailToFile(context, email);
			}catch (Exception e){
				logger.severe("fail to write email to file");
			}
		}
		else {
			String subject = "[Airlock]" + separator + stage + " CHANGES" + separator + "\""+ featureToUpdate.getName()+
					"\"" + separator + product.getName() + separator +"["+ versionRange +"]"+ separator + "("+ serverName+ ")";


			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
			Date now = new Date();
			String changeDate = sdfDate.format(now);
			String rootCauseString = "<br/>";
			if(rootCause != null && featureToUpdate.getUniqueId() != apiCallID){
				rootCauseString = "Event cause: <strong>" + rootCause +"</strong><br/><br/>";
			}
			String consoleString = getConsoleString(context);
			String mailBody = "<strong> Details: </strong><br/>"+
					"User:<strong> "+ userId +"</strong><br/>"+
					"Date:<strong> " + changeDate + "</strong><br/>"+
					"Product:<strong> " + product.getName()+ "</strong><br/>"+
					"Version Range: <strong>"+ versionRange+ "</strong><br/>"+
					"Branch:<strong> " + branchName+ "</strong><br/>"+
					"Feature: <strong>"+featureToUpdate.getNameSpaceDotName()+"</strong><br/>"+
					rootCauseString+
					"<strong>Update: <br/>" +
					details.replace("\n","<br/>") +"</strong><br/><br/>"+
					"<strong>Additional info:</strong> <br/>"+
					consoleString +
					"Server:<strong> "+ serverName +"</strong><br/>"+
					"Product ID:<strong> " + product.getUniqueId()+ "</strong><br/>"+
					"Version range ID: <strong>"+ season.getUniqueId()+ "</strong><br/>"+
					"Feature ID: <strong>"+featureToUpdate.getUniqueId()+"</strong> <br/>"+
					"Minimum App Version: <strong>"+ minAppVersion+"</strong> <br/>"+
					"Default if down: <strong>"+ defaultIfDown+"</strong> <br/>"+
					"In case of rule error, use last known value: <strong>"+ cachedResults+"</strong> <br/><br/>";
			sendEmail(context, emailFollowers, subject, mailBody);
		}
	}

	public static void writeMailToFile(ServletContext context,JSONObject email){
		//writeToFile
		ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock)context.getAttribute(Constants.GLOBAL_LOCK_PARAM_NAME);
		readWriteLock.writeLock().lock();
		try {
			DataSerializer ds = (DataSerializer) context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String pathSeparator = ds.getSeparator();
			JSONObject allEmails = new JSONObject();
			JSONArray allEmailsArr = new JSONArray();
			String emailFile = Constants.TESTS_PATH +pathSeparator+Constants.EMAILS_FILE_NAME;
			if (ds.isFileExists(emailFile)) {
				allEmailsArr = ds.readDataToJSON(emailFile).getJSONArray("allEmails");
			}
			allEmailsArr.add(email);
			allEmails.put("allEmails", allEmailsArr);
			ds.writeData(emailFile, allEmails.write(true));
		} catch (IOException ioe) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = Strings.failedWritingProduct + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			//throw new IOException(error);
		} catch (JSONException je) {
			//failed writing roles 3 times to s3.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
			String error = Strings.failedWritingProduct + je.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
			//throw new IOException(error);
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}
	public static void sendEmailFromAzure(ServletContext context,ArrayList<String> followers,String subject,String body) {
		String sendGridApiKey = (String)context.getAttribute(Constants.ENV_SEND_GRID_API_KEY);		

		// String SENDGRID_API_KEY = "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";
		SendGrid sendgrid = new SendGrid(sendGridApiKey);

		SendGrid.Email email = new SendGrid.Email();
		String[] addresses = followers.toArray(new String[followers.size()]);

		String from = (String)context.getAttribute(Constants.AIRLOCK_CHANGES_MAIL_ADDRESS_PARAM_NAME);

		try {
			for (String toAddress:addresses) {
				email.addTo(toAddress);
			}
			email.setFrom(from);
			email.setSubject(subject);
			email.setHtml(body);

			SendGrid.Response response = sendgrid.send(email);
			if (response.getCode()!=200) {
				logger.severe("The email was not sent: " + response.getMessage());		
			}
		} catch (SendGridException e) {
			logger.severe("The email was not sent: " + e.getMessage());		
		}
	}

	public static void sendEmail(ServletContext context,ArrayList<String> followers,String subject,String body) {
		Constants.EmailProviderType emailProviderType  = (Constants.EmailProviderType)context.getAttribute(Constants.ENV_EMAIL_PROVIDER_TYPE);

		if (emailProviderType.equals(Constants.EmailProviderType.SEND_GRID)) {
			sendEmailFromAzure(context, followers, subject, body);
		}
		else if (emailProviderType.equals(Constants.EmailProviderType.AWS)) {
			sendEmailFromAWS(context, followers, subject, body);	
		} 

	}

	public static void sendEmailFromAWS(ServletContext context,ArrayList<String> followers,String subject,String body) {
		String[] addresses = followers.toArray(new String[followers.size()]);

		String from = (String)context.getAttribute(Constants.AIRLOCK_CHANGES_MAIL_ADDRESS_PARAM_NAME);
		// Construct an object to contain the recipient address.
		Destination destination = new Destination().withToAddresses(addresses);

		// Create the subject and body of the message.
		Content subjectMail = new Content().withData(subject);
		Content textBody = new Content().withData(body);
		Body bodyMail = new Body().withHtml(textBody);


		// Create a message with the specified subject and body.
		com.amazonaws.services.simpleemail.model.Message message = new com.amazonaws.services.simpleemail.model.Message().withSubject(subjectMail).withBody(bodyMail);

		// Assemble the email.
		SendEmailRequest request = new SendEmailRequest().withSource(from).withDestination(destination).withMessage(message);

		try {

			// Instantiate an Amazon SES client, which will make the service call. The service call requires your AWS credentials.
			// Because we're not providing an argument when instantiating the client, the SDK will attempt to find your AWS credentials
			// using the default credential provider chain. The first place the chain looks for the credentials is in environment variables
			// AWS_ACCESS_KEY_ID and AWS_SECRET_KEY.
			// For more information, see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
			AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient();

			// Choose the AWS region of the Amazon SES endpoint you want to connect to. Note that your sandbox
			// status, sending limits, and Amazon SES identity-related settings are specific to a given AWS
			// region, so be sure to select an AWS region in which you set up Amazon SES. Here, we are using
			// the US West (Oregon) region. Examples of other regions that Amazon SES supports are US_EAST_1
			// and EU_WEST_1. For a complete list, see http://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html
			String regionName = (String)context.getAttribute(Constants.SES_ENDPOINT);
			Region REGION = Region.getRegion(Regions.fromName(regionName));
			client.setRegion(REGION);

			// Send the email.
			client.sendEmail(request);
			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Email sent!", null);
		} catch (Exception ex) {
			logger.severe("The email was not sent: " + ex.getMessage());
		}
	}

	public static String initFromSeasonBranchesJSON(JSONObject branchesJSON, ServletContext context, DataSerializer ds) throws JSONException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);
		@SuppressWarnings("unchecked")
		Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

		String pathSeparator = ds.getSeparator();

		String seasonId = branchesJSON.getString(Constants.JSON_FEATURE_FIELD_SEASON_ID);		
		try {

			Season season = seasonsDB.get(seasonId);

			if (season == null) {
				throw new IllegalArgumentException("season '" + seasonId + "' was not found in the products.json file."); 
			}

			Environment env = new Environment();
			env.setServerVersion(season.getServerVersion()); 

			//for now no validation is done - error will cause JSONError.
			JSONArray branchesArr = branchesJSON.getJSONArray(Constants.JSON_FIELD_BRANCHES);
			season.getBranches().fromJSON(branchesArr, branchesDB, env, context);
			for (Branch branch:season.getBranches().getBranchesList()) {
				Environment branchEnv = new Environment();
				env.setBranchId(branch.getUniqueId().toString());
				env.setServerVersion(season.getServerVersion());

				String branchId = branch.getUniqueId().toString();

				String branchFeaturesPath = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+
						pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
						pathSeparator+branchId+pathSeparator+Constants.AIRLOCK_BRANCH_FEATURES_FILE_NAME;

				JSONObject branchJSON = ds.readDataToJSON(branchFeaturesPath);
				env.setServiceState(ServiceState.INITIALIZING);
				branch.fromJSON(branchJSON, env, season, context);

				//load branch entitlements
				String branchEntitlementsPath = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+
						pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
						pathSeparator+branchId+pathSeparator+Constants.AIRLOCK_BRANCH_ENTITLEMENTS_FILE_NAME;

				if (ds.isFileExists(branchEntitlementsPath)) {
					JSONObject branchEntitlementsJSON = ds.readDataToJSON(branchEntitlementsPath);
					branch.fromEntitlementsJSON(branchEntitlementsJSON, env, season, context);
				}
				
				//Branch analytics
				String branchAnalyticsPath = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+
						pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.AIRLOCK_BRANCHES_FOLDER_NAME+ 
						pathSeparator+branchId+pathSeparator+Constants.AIRLOCK_BRANCH_ANALYTICS_FILE_NAME;
				
				JSONObject branchAnalyticsJSON = ds.readDataToJSON(branchAnalyticsPath);

				Map<String, BaseAirlockItem> airlockItemsDB  = Utilities.getAirlockItemsDB(branchId, context);
				branch.getAnalytics().fromJSON(season.getUniqueId(), branchAnalyticsJSON, context, branchEnv, airlockItemsDB);
			}
			return seasonId;
		} catch (JSONException jsne) {
			throw new JSONException("Failed parsing season '" + seasonId +  "' JSON: " + jsne.getMessage());
		} catch (MergeException e) {
			throw new JSONException(Strings.mergeException  + e.getMessage());
		}  
	}

	//return list of the updated branches ids
	public static void duplicateDeletedItemWhenCheckedOutInBranches(BaseAirlockItem featureToDel, Season season, ServletContext context, TreeSet<String> changedBranches, Map<String, Map<String, BaseAirlockItem>> branchAirlockItemsDBsMap, Constants.REQUEST_ITEM_TYPE reqType) throws MergeException {

		for (Branch b:season.getBranches().getBranchesList()) {	
			BaseAirlockItem featureInBranch = b.getBranchAirlockItemsBD().get(featureToDel.getUniqueId().toString());
			boolean isCheckedOutFeaure = (featureInBranch!=null && featureInBranch.getBranchStatus().equals(BranchStatus.CHECKED_OUT));
			boolean isParentOfSubTree = b.featureIsParentOfBranchSubTree(featureToDel, reqType);
			if (isCheckedOutFeaure || isParentOfSubTree) {				
				//duplicate deleted feature in branch if is checked out or parent of some subtree in the branch
				if (isCheckedOutFeaure) {
					changedBranches.add(b.getUniqueId().toString());
					featureInBranch.setBranchStatus(BranchStatus.NEW);
					featureInBranch.setUniqueId(UUID.randomUUID());
					updateNewIdInSubItems(featureInBranch);
					if (reqType.equals(REQUEST_ITEM_TYPE.FEATURES)) {
						updateNewIdInOrderingRules(featureInBranch, b, branchAirlockItemsDBsMap, featureToDel.getUniqueId().toString(), featureInBranch.getUniqueId().toString(), context);
					}
					else {
						updateNewPurchaseIdInFeatures(featureInBranch, b, featureToDel.getUniqueId().toString(), featureInBranch.getUniqueId().toString(), context);
						updateNewPurchaseIdInPurcahseBundles(featureInBranch, b, featureToDel.getUniqueId().toString(), featureInBranch.getUniqueId().toString(), context);
					}
					b.getBranchAirlockItemsBD().remove(featureToDel.getUniqueId().toString());
					b.getBranchAirlockItemsBD().put(featureInBranch.getUniqueId().toString(), featureInBranch);

					if (b.isFeatureInAnalytics(featureToDel)) {
						b.replaceFeatureIdInAnalytics(featureToDel.getUniqueId().toString(), featureInBranch.getUniqueId().toString());
					}
				} else {
					//parent of subTree - duplicate in branch
					b.duplicateAndCheckoutToBranch(featureToDel, context, season, reqType);
				}

				if (featureToDel instanceof BaseMutualExclusionGroupItem) {
					//since we updated the id - we should go over all the branch features and see if they are pointing to it,
					//if so, update the name to include the new id
					String newMtxId = featureInBranch.getUniqueId().toString();
					for (BaseAirlockItem subTreeRoot:b.getBranchFeatures()) {
						if (subTreeRoot.getBranchFeatureParentName().startsWith("mx.")) {
							//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
							//subTree root as well.
							String prevMtxId = subTreeRoot.getBranchFeatureParentName().substring(3);
							if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
								subTreeRoot.setBranchFeatureParentName("mx." + newMtxId);
							}
						}
					}

					Set<String> branchFeatureIds = b.getBranchAirlockItemsBD().keySet();
					for (String branchFeatureId:branchFeatureIds) {
						BaseAirlockItem branchFeature = b.getBranchAirlockItemsBD().get(branchFeatureId);
						if (branchFeature.getBranchFeaturesItems()!=null) {
							for (int i=0; i<branchFeature.getBranchFeaturesItems().size(); i++) {
								String subFeatureId = branchFeature.getBranchFeaturesItems().get(i); 
								if (subFeatureId.startsWith("mx.")) {
									//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
									//subTree root as well.
									String prevMtxId = subFeatureId.substring(3);
									if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
										subFeatureId = "mx." + newMtxId;
										branchFeature.getBranchFeaturesItems().set(i, subFeatureId);
									}
								}

							}
						}

						if (branchFeature.getBranchConfigurationRuleItems()!=null) {
							for (int i=0; i<branchFeature.getBranchConfigurationRuleItems().size(); i++) {
								String subConfigId = branchFeature.getBranchConfigurationRuleItems().get(i); 
								if (subConfigId.startsWith("mx.")) {
									//it is pointing to a mutualExclusion item. Since the mtx id was changed we should update it in the 
									//subTree root as well.
									String prevMtxId = subConfigId.substring(3);
									if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
										subConfigId = "mx." + newMtxId;
										branchFeature.getBranchConfigurationRuleItems().set(i, subConfigId);
									}
								}

							}
						}

						if (branchFeature.getBranchOrderingRuleItems()!=null) {
							for (int i=0; i<branchFeature.getBranchOrderingRuleItems().size(); i++) {
								String subConfigId = branchFeature.getBranchOrderingRuleItems().get(i); 
								if (subConfigId.startsWith("mx.")) {
									//it is pointing to a OrderingRuleMutualExclusion item. Since the mtx id was changed we should update it in the 
									//subTree root as well.
									String prevMtxId = subConfigId.substring(3);
									if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
										subConfigId = "mx." + newMtxId;
										branchFeature.getBranchOrderingRuleItems().set(i, subConfigId);
									}
								}

							}
						}
						
						if (branchFeature.getBranchEntitlementItems()!=null) {
							for (int i=0; i<branchFeature.getBranchEntitlementItems().size(); i++) {
								String subItemId = branchFeature.getBranchEntitlementItems().get(i); 
								if (subItemId.startsWith("mx.")) {
									//it is pointing to a InAppPurchaseMutualExclusion item. Since the mtx id was changed we should update it in the 
									//subTree root as well.
									String prevMtxId = subItemId.substring(3);
									if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
										subItemId = "mx." + newMtxId;
										branchFeature.getBranchEntitlementItems().set(i, subItemId);
									}
								}

							}
						}
	
						if (branchFeature.getBranchPurchaseOptionsItems()!=null) {
							for (int i=0; i<branchFeature.getBranchPurchaseOptionsItems().size(); i++) {
								String subItemId = branchFeature.getBranchPurchaseOptionsItems().get(i); 
								if (subItemId.startsWith("mx.")) {
									//it is pointing to a PurchaseOptionsMutualExclusion item. Since the mtx id was changed we should update it in the 
									//subTree root as well.
									String prevMtxId = subItemId.substring(3);
									if (prevMtxId.equals(featureToDel.getUniqueId().toString())) {
										subItemId = "mx." + newMtxId;
										branchFeature.getBranchPurchaseOptionsItems().set(i, subItemId);
									}
								}

							}
						}

					}			

				}
			}			
		}	

		if (featureToDel.getFeaturesItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getFeaturesItems()) {
				duplicateDeletedItemWhenCheckedOutInBranches(subItem, season, context, changedBranches, branchAirlockItemsDBsMap, reqType);
			}
		}

		if (featureToDel.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getConfigurationRuleItems()) {
				duplicateDeletedItemWhenCheckedOutInBranches(subItem, season, context, changedBranches, branchAirlockItemsDBsMap, reqType);
			}
		}
		
		if (featureToDel.getEntitlementItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getEntitlementItems()) {
				duplicateDeletedItemWhenCheckedOutInBranches(subItem, season, context, changedBranches, branchAirlockItemsDBsMap, reqType);
			}
		}
		
		if (featureToDel.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getPurchaseOptionsItems()) {
				duplicateDeletedItemWhenCheckedOutInBranches(subItem, season, context, changedBranches, branchAirlockItemsDBsMap, reqType);
			}
		}
	}

	private static void updateNewPurchaseIdInFeatures(BaseAirlockItem featureInBranch, Branch branch, String oldId, String newId, ServletContext context) throws MergeException {
		
		if (!(featureInBranch instanceof EntitlementItem))
			return; //only inAppPurcahses can be attached to feature

		Set<String> branchItemsIds = branch.getBranchAirlockItemsBD().keySet();
		for(String itemId:branchItemsIds) {
			BaseAirlockItem alItem = branch.getBranchAirlockItemsBD().get(itemId);
			if (alItem.getType().equals(Type.FEATURE)) {
				FeatureItem fi = (FeatureItem) alItem;
				if (fi.getEntitlement()!=null && fi.getEntitlement().equals(oldId)) {
					fi.setEntitlement(newId);
				}
			}
		}
	}

	private static void updateNewPurchaseIdInPurcahseBundles(BaseAirlockItem featureInBranch, Branch branch, String oldId, String newId, ServletContext context) throws MergeException {
		if (!(featureInBranch instanceof EntitlementItem))
			return; //only inAppPurcahses can be part of purchases bundles

		Set<String> branchItemsIds = branch.getBranchAirlockItemsBD().keySet();
		for(String itemId:branchItemsIds) {
			BaseAirlockItem alItem = branch.getBranchAirlockItemsBD().get(itemId);
			if (alItem.getType().equals(Type.ENTITLEMENT)) {
				EntitlementItem iap = (EntitlementItem) alItem;
				if (iap.getIncludedPurchases()!=null && !iap.getIncludedPurchases().isEmpty()) {
					for (int i=0; i<iap.getIncludedPurchases().size(); i++) {
						if (iap.getIncludedPurchases().get(i).equals(oldId)) {
							iap.getIncludedPurchases().set(i, newId);
							break;
						}
					}
				}
			}
		}
	}

	private static void updateNewIdInOrderingRules(BaseAirlockItem featureInBranch, Branch branch, Map<String, Map<String, BaseAirlockItem>> branchAirlockItemsDBsMap, String oldId, String newId, ServletContext context) throws MergeException {
		if (!(featureInBranch instanceof FeatureItem) && !(featureInBranch instanceof MutualExclusionGroupItem))
			return; //only mtx or features appears in orderingRule configuration

		Map<String, BaseAirlockItem> branchAirlockItemsDB = null;
		if (!branchAirlockItemsDBsMap.containsKey(branch.getUniqueId().toString())) {
			branchAirlockItemsDB = Utilities.getAirlockItemsDB(branch.getUniqueId().toString(), context);
			branchAirlockItemsDBsMap.put(branch.getUniqueId().toString(), branchAirlockItemsDB);
		}
		else {
			branchAirlockItemsDB = branchAirlockItemsDBsMap.get(branch.getUniqueId().toString());
		}

		if (featureInBranch.getParent() == null) //parent is null if the parent is the root in branch and under the root there are no ordering rule 
			return;

		BaseAirlockItem parentFeatureInBranch = branchAirlockItemsDB.get(featureInBranch.getParent().toString());
		if (parentFeatureInBranch!=null) { //should never be null but just in case
			if (parentFeatureInBranch.getOrderingRuleItems()!=null) {
				for (int i=0; i<parentFeatureInBranch.getOrderingRuleItems().size(); i++) {
					BaseAirlockItem curOrderingRule = parentFeatureInBranch.getOrderingRuleItems().get(i);
					if (curOrderingRule.getBranchStatus().equals(BranchStatus.CHECKED_OUT) || curOrderingRule.getBranchStatus().equals(BranchStatus.NEW)) {
						BaseAirlockItem curOrderingRuleInBranch = branch.getBranchAirlockItemsBD().get(curOrderingRule.getUniqueId().toString());
						OrderingRuleItem.replaceIdsInSubTree(curOrderingRuleInBranch, oldId, newId);
					}

				}
			}
		}
	}	

	//map between server version and defaults file version 2.1=>V2.1 2.5=>V2.5. 3.0=>2.5	
	public static String getSDKVersionPerSerevrVersion(String serverVersion) {
		if (serverVersion.equals(Constants.PRE_2_1_SERVER_VERSION))
			return "V2";

		if (serverVersion.equals("2.1"))
			return "V2.1";

		if (serverVersion.equals("2.5"))
			return "V2.5";

		if (serverVersion.equals("3.0"))
			return "V2.5";

		if (serverVersion.equals("4.0"))

			return "V2.5"; 

		if (serverVersion.equals("4.1"))
			return "V2.5"; 

		if (serverVersion.equals("4.5"))  
			return "V2.5"; 

		if (serverVersion.equals("5.0"))  
			return "V2.5"; 

		if (serverVersion.equals("5.5"))  
			return "V2.5"; 


		return null;
	}

	//if season is given - validate that branch is in the given season
	public static ValidationResults validateBranchId(ServletContext context, String branch_id, Season season) {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) 
			return null;

		if (branch_id.equals(Constants.MASTER_BRANCH_NAME.toLowerCase())) {
			String errMsg = Strings.illegalBranchName;
			logger.severe(errMsg);
			return new ValidationResults(errMsg, Status.BAD_REQUEST);
		}

		@SuppressWarnings("unchecked")
		Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

		if (branchesDB.get(branch_id) == null) {
			String errMsg = Strings.nonExistingBranch;
			logger.severe(errMsg);
			return new ValidationResults(errMsg, Status.BAD_REQUEST);
		}

		if (season!=null) {
			for (Branch b:season.getBranches().getBranchesList()) {
				if (b.getUniqueId().toString().equals(branch_id))
					return null;
			}

			String errMsg = Strings.branchNotInSeason;
			logger.severe(errMsg);
			return new ValidationResults(errMsg, Status.BAD_REQUEST);
		}

		return null;
	}

	//return the relevant airlockItemsDB - for master return the one on the context, for branch return the one computed while 
	//merging the branch tree
	@SuppressWarnings("unchecked")
	public static Map<String, BaseAirlockItem> getAirlockItemsDB(String branch_id, ServletContext context) throws MergeException {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);						
		}
		else {
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);
			Season season = seasonsDB.get(branch.getSeasonId().toString());
			BaseAirlockItem branchRoot = MergeBranch.merge(season.getRoot(), branch, Constants.REQUEST_ITEM_TYPE.FEATURES);
			Map<String, BaseAirlockItem> res = MergeBranch.getItemMap(branchRoot, true);

			//add merge of the branch purchases
			BaseAirlockItem branchPurchaseRoot = MergeBranch.merge(season.getEntitlementsRoot(), branch, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); 
			MergeBranch.addToItemMap(branchPurchaseRoot, true, res);
			return res;
		}
	}

	/*
	//return the relevant purchaseItemsDB - for master return the one on the context, for branch return the one computed while 
	//merging the branch tree
	@SuppressWarnings("unchecked")
	public static Map<String, BaseAirlockItem> getPurchaseItemsDB(String branch_id, ServletContext context) throws MergeException {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);						
		}
		else {
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);
			Season season = seasonsDB.get(branch.getSeasonId().toString());
			BaseAirlockItem branchRoot = MergeBranch.merge(season.getInAppPurchasesRoot(), branch); 
			return MergeBranch.getItemMap(branchRoot, true);				
		}
	}*/

	public static Map<String, BaseAirlockItem> getAirlockItemsDBForBranchCopy(Branch branchCopy, ServletContext context) throws MergeException {
		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Season season = seasonsDB.get(branchCopy.getSeasonId().toString());
		
		BaseAirlockItem branchPurchasesRoot = MergeBranch.merge(season.getEntitlementsRoot(), branchCopy, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); 
		Map<String, BaseAirlockItem> airlockPurchasesItemsDB = MergeBranch.getItemMap(branchPurchasesRoot, true);
		
		BaseAirlockItem branchRoot = MergeBranch.merge(season.getRoot(), branchCopy, Constants.REQUEST_ITEM_TYPE.FEATURES); 
		Map<String, BaseAirlockItem> airlockFeaturesItemsDB = MergeBranch.getItemMap(branchRoot, true);
		
		Map<String, BaseAirlockItem> uniteAirlockItemsDB = new HashMap<String, BaseAirlockItem>();
		uniteAirlockItemsDB.putAll(airlockPurchasesItemsDB);
		uniteAirlockItemsDB.putAll(airlockFeaturesItemsDB);
		
		return uniteAirlockItemsDB;
	}

	//return the relevant airlockAnalytics - for master return the one on the season, for branch return the one computed while 
	//merging the master and branch analytics
	public static AirlockAnalytics getAirlockAnalytics(Season season, String branch_id, ServletContext context, Map<String, BaseAirlockItem> airlockItemsDB, Environment env) throws MergeException {
		if (branch_id.equals(Constants.MASTER_BRANCH_NAME)) {
			return season.getAnalytics();
		}
		else {			
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branch_id);		//i know branch exists - after validate

			AirlockAnalytics masterAirlockAnalytics = season.getAnalytics();
			AirlockAnalytics branchAirlockAnalytics = branch.getAnalytics();

			AirlockAnalytics mergedAirlockAnalytics  = new AirlockAnalytics(season.getUniqueId());

			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);


			mergedAirlockAnalytics = Utilities.mergeAnalytics(mergedAirlockAnalytics, masterAirlockAnalytics, season, context, masterAirlockItemsDB, env);
			return Utilities.mergeAnalytics(mergedAirlockAnalytics, branchAirlockAnalytics, season, context, airlockItemsDB, env);
		}			
	}

	public static AirlockAnalytics mergeAnalytics(AirlockAnalytics mainAirlockAnalytics, AirlockAnalytics additionalAirlockAnalytics, Season season, ServletContext context, Map<String, BaseAirlockItem> branchAirlockItemsDB, Environment env) throws MergeException {
		try {
			//AirlockAnalytics mergedAirlockAnalytics = masterAirlockAnalytics.duplicateForNewSeason(season.getUniqueId(), null, context, season.getUniqueId());
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> masterAirlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);


			AirlockAnalytics mergedAirlockAnalytics = mainAirlockAnalytics.duplicateForNewSeason(season.getUniqueId(), null, context, season.getUniqueId());

			AnalyticsDataCollection mergedDataCollection = mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection();
			AnalyticsDataCollection branchDataCollection = additionalAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection();

			//merge features on/off

			//first remove from analytics feature that does not exists in branch (not even none)
			//they were added to master after parent checkout
			List<String> existingFeaturesOnOff = Utilities.cloneStringsList(mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesOnOff());
			for (String existingFeatureOnOff:existingFeaturesOnOff) {
				BaseAirlockItem bai = branchAirlockItemsDB.get(existingFeatureOnOff);
				if (bai == null) {
					//the feature does not exists in the branch - only in master (was created after parent checkout)
					mergedAirlockAnalytics.getGlobalDataCollection().removeFeatureOnOff(masterAirlockItemsDB.get(existingFeatureOnOff), masterAirlockItemsDB);
				}
			}

			LinkedList<String> branchFeaturesOnOff = branchDataCollection.getFeaturesOnOff();
			for (String branchFeatureOnOff : branchFeaturesOnOff) {
				BaseAirlockItem bai = branchAirlockItemsDB.get(branchFeatureOnOff);
				if (bai == null) {
					//the feature does not exists in the branch - should not happen
					continue;
				}
				if (!mergedDataCollection.getFeaturesOnOffMap().containsKey(branchFeatureOnOff)) {					
					mergedAirlockAnalytics.getGlobalDataCollection().addFeatureOnOff(bai, branchAirlockItemsDB);
				}
				else {
					//featureOnOff already in master. verify stage for counters  					
					BaseAirlockItem itemInMaster = masterAirlockItemsDB.get(branchFeatureOnOff);
					if (itemInMaster!=null && itemInMaster instanceof DataAirlockItem) {
						Stage stageInMaster = ((DataAirlockItem)itemInMaster).getStage();
						Stage stageInBranch = ((DataAirlockItem)bai).getStage();
						if (!stageInMaster.equals(stageInBranch)) {
							int prodCounter = mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().getNumberOfProductionItemsToAnalytics();									
							if (stageInMaster.equals(Stage.PRODUCTION)) {
								//move to dev in branch - decrease production counter
								mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().setNumberOfProductionItemsToAnalytics(prodCounter-1);
							}
							else {
								//move to prod in branch - increase production counter
								mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().setNumberOfProductionItemsToAnalytics(prodCounter+1);
							}
						}
					}

				}
			}

			//merge input fields
			LinkedList<String> branchInputFields = branchDataCollection.getInputFieldsForAnalytics();			
			for (String branchInputField : branchInputFields) {
				if (!mergedDataCollection.getInputFieldsForAnalyticsStageMap().containsKey(branchInputField)) {					
					/*mergedDataCollection.getInputFieldsForAnalytics().add(branchInputField);
					mergedDataCollection.getInputFieldsForAnalyticsStageMap().put(branchInputField, branchDataCollection.getInputFieldsForAnalyticsStageMap().get(branchInputField));*/
					mergedDataCollection.addInputField(branchInputField, branchDataCollection.getInputFieldsForAnalyticsStageMap().get(branchInputField));
				}
			}

			//merge feature attributes

			//first remove from analytics feature that does not exists in branch (not even none)
			//they were added to master after parent checkout
			Set<String> existingFeaturesAttributes = cloneStringsSet(mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().getFeaturesConfigurationAttributesMap().keySet());

			for (String existingFeatureAtt:existingFeaturesAttributes) {
				BaseAirlockItem bai = branchAirlockItemsDB.get(existingFeatureAtt);
				if (bai == null) {
					//the feature does not exists in the branch - only in master (was created after parent checkout)
					mergedAirlockAnalytics.getGlobalDataCollection().getAnalyticsDataCollection().removeFeaturAttributesList(masterAirlockItemsDB.get(existingFeatureAtt), branchAirlockItemsDB);
				}
			}


			LinkedList<FeatureAttributesPair> branchFeatureAttributesList = branchDataCollection.getFeaturesConfigurationAttributesList();
			if (branchFeatureAttributesList!=null && branchFeatureAttributesList.size()>0) {
				// build the att map from the merged features tree:
				BaseAirlockItem featuresRootOfMergedTree = branchAirlockItemsDB.get(season.getRoot().getUniqueId().toString());
				BaseAirlockItem entitlementsRootOfMergedTree = branchAirlockItemsDB.get(season.getRoot().getUniqueId().toString());				
				
				Map<String, TreeSet<String>> featureAttributesMap = AnalyticsDataCollection.getFeatureAttributesMap (featuresRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), entitlementsRootOfMergedTree.toJson(OutputJSONMode.ADMIN, context, env), season, context);

				for (FeatureAttributesPair branchFeatureAtts:branchFeatureAttributesList) {
					if ((FeatureItem)branchAirlockItemsDB.get(branchFeatureAtts.id) == null) {
						//the feature does not exists in the branch (only master feature)- ignore
						continue;
					}
					boolean isProductionFeatureInBranch = ((FeatureItem)branchAirlockItemsDB.get(branchFeatureAtts.id)).getStage().equals(Stage.PRODUCTION);
					boolean isProductionFeatureInMaster = isProductionFeatureInBranch; //if new branch - treat master counters as in branch
					if (masterAirlockItemsDB.get(branchFeatureAtts.id)!=null) {
						isProductionFeatureInMaster = ((FeatureItem)masterAirlockItemsDB.get(branchFeatureAtts.id)).getStage().equals(Stage.PRODUCTION);
					}
					mergedDataCollection.addDeltaFeatureAttsPair(branchFeatureAtts, featureAttributesMap, season, env, isProductionFeatureInBranch, isProductionFeatureInMaster);
				}
			}

			//The last modify is the branch last modify since it is the analytics that is going to be changed
			mergedAirlockAnalytics.getGlobalDataCollection().setLastModified(additionalAirlockAnalytics.getGlobalDataCollection().getLastModified());
			return mergedAirlockAnalytics;
		} catch (JSONException je) {
			throw new MergeException("Analytics merge error: " + je.getMessage());
		}
	}


	public static JSONObject getAllFeaturesList(Season season) throws JSONException {
		JSONObject res = new JSONObject();
		JSONArray featuresArr = new JSONArray();

		//TODO: only features or configurationRules as well?
		addFeatureToList(season.getRoot(), featuresArr, false);

		for (Branch branch:season.getBranches().getBranchesList()) {
			for (BaseAirlockItem branchSubTreeRoot:branch.getBranchFeatures()) {
				addFeatureToList(branchSubTreeRoot, featuresArr, true);
			}
		}

		res.put(Constants.JSON_FEATURE_FIELD_FEATURES, featuresArr);
		return res;
	}

	private static void addFeatureToList(BaseAirlockItem item, JSONArray featuresArr, boolean inBranch) throws JSONException {
		if (item instanceof FeatureItem) {
			if (!inBranch || item.getBranchStatus().equals(BranchStatus.NEW) ) { //in branch add only new features
				JSONObject featureJson = new JSONObject();
				featureJson.put(Constants.JSON_FIELD_UNIQUE_ID, item.getUniqueId().toString());
				featureJson.put(Constants.JSON_FIELD_NAME, ((FeatureItem)item).getName());
				featureJson.put(Constants.JSON_FEATURE_FIELD_NAMESPACE, ((FeatureItem)item).getNamespace());
				featuresArr.add(featureJson);
			}
		}

		if(item.getFeaturesItems()!=null) {
			for (BaseAirlockItem subItem:item.getFeaturesItems()) {
				addFeatureToList(subItem, featuresArr, inBranch);
			}
		}		
	}

	public static LinkedList<String> cloneStringsList(LinkedList<String> srcList) {
		if (srcList == null)
			return null;
		LinkedList<String> res = new LinkedList<String>();
		for(String s:srcList) {
			res.add(s);
		}
		return res;
	}

	public static LinkedList<RoleType> cloneRoleTypesList(List<RoleType> srcList) {
		if (srcList == null)
			return null;
		LinkedList<RoleType> res = new LinkedList<RoleType>();
		for(RoleType s:srcList) {
			res.add(s);
		}
		return res;
	}

	public static ArrayList<JSONObject> cloneJSONObjectsList(ArrayList<JSONObject> srcList) {
		if (srcList == null)
			return null;
		ArrayList<JSONObject> res = new ArrayList<JSONObject>();
		for(JSONObject obj:srcList) {
			res.add((JSONObject)obj.clone());
		}
		return res;
	}

	public static Set<String> cloneStringsSet(Set<String> srcSet) {
		if (srcSet == null)
			return null;
		Set<String> res = new HashSet<String>();
		for(String s:srcSet) {
			res.add(s);
		}
		return res;
	}

	public static Set<AirlockCapability> cloneCapabilitiesSet(Set<AirlockCapability> srcSet) {
		if (srcSet == null)
			return null;
		Set<AirlockCapability> res = new HashSet<AirlockCapability>();
		for(AirlockCapability s:srcSet) {
			res.add(s);
		}
		return res;
	}

	public static TreeSet<String> stringsListToTreeSet(LinkedList<String> stringsList) {
		TreeSet<String> res = new TreeSet<String>();
		for (String s:stringsList) {
			res.add(s);
		}
		return res;
	}

	public static TreeSet<String> jsonArrayToTreeSet(JSONArray stringsArray) throws JSONException {
		TreeSet<String> res = new TreeSet<String>();
		for (int i=0; i<stringsArray.size(); i++ ) {
			res.add(stringsArray.getString(i));
		}
		return res;
	}


	public static LinkedList<AttributeTypePair> cloneAttributesList(LinkedList<AttributeTypePair> attributesList) {
		if (attributesList==null)
			return null;

		LinkedList<AttributeTypePair> res = new LinkedList<AttributeTypePair>();

		for (AttributeTypePair atp:attributesList) {
			res.add(atp.duplicateForNewSeason());
		}

		return res;		
	}

	//only when the deleted feature is none in the branch - it should be deleted from lists since removed from branch as well.	
	//checkedOut features are duplicated in branch when deleted in master
	public static void removeDeletedfromBranchSubItemLists(BaseAirlockItem featureToDel, Season season, TreeSet<String> changedBranches) {
		String delFeatureName = Branch.getItemBranchName(featureToDel);
		for (Branch b:season.getBranches().getBranchesList()) {			
			boolean found = false;
			Set<String> brancheFeaturesIds = b.getBranchAirlockItemsBD().keySet();
			for (String branchFeatureId : brancheFeaturesIds) {
				BaseAirlockItem branchFeatureByName = b.getBranchFeatureByName(delFeatureName);
				if (branchFeatureByName!=null && branchFeatureByName.getBranchStatus().equals(BranchStatus.NEW)) {
					continue; //the feature was duplicated in the branch - no need to remove from lists
				}

				BaseAirlockItem branchItem = b.getBranchAirlockItemsBD().get(branchFeatureId);				
				if (branchItem.getBranchFeaturesItems()!=null) {
					for (int i=0; i<branchItem.getBranchFeaturesItems().size(); i++) {
						if (branchItem.getBranchFeaturesItems().get(i).equals(delFeatureName)) {
							branchItem.getBranchFeaturesItems().remove(i);
							changedBranches.add(b.getUniqueId().toString());
							found = true;
							break;
						}
					}
				}
				if (!found && branchItem.getBranchConfigurationRuleItems()!=null) {
					for (int i=0; i<branchItem.getBranchConfigurationRuleItems().size(); i++) {
						if (branchItem.getBranchConfigurationRuleItems().get(i).equals(delFeatureName)) {
							branchItem.getBranchConfigurationRuleItems().remove(i);
							changedBranches.add(b.getUniqueId().toString());
							found = true;
							break;
						}
					}
				}
				if (!found && branchItem.getBranchOrderingRuleItems()!=null) {
					for (int i=0; i<branchItem.getBranchOrderingRuleItems().size(); i++) {
						if (branchItem.getBranchOrderingRuleItems().get(i).equals(delFeatureName)) {
							branchItem.getBranchOrderingRuleItems().remove(i);
							changedBranches.add(b.getUniqueId().toString());
							found = true;
							break;
						}
					}
				}
				if (!found && branchItem.getBranchEntitlementItems()!=null) {
					for (int i=0; i<branchItem.getBranchEntitlementItems().size(); i++) {
						if (branchItem.getBranchEntitlementItems().get(i).equals(delFeatureName)) {
							branchItem.getBranchEntitlementItems().remove(i);
							changedBranches.add(b.getUniqueId().toString());
							found = true;
							break;
						}
					}
				}
				if (!found && branchItem.getBranchPurchaseOptionsItems()!=null) {
					for (int i=0; i<branchItem.getBranchPurchaseOptionsItems().size(); i++) {
						if (branchItem.getBranchPurchaseOptionsItems().get(i).equals(delFeatureName)) {
							branchItem.getBranchPurchaseOptionsItems().remove(i);
							changedBranches.add(b.getUniqueId().toString());
							found = true;
							break;
						}
					}
				}
				if (found) //if delFeature is found as a child of one feature in branch - no need to look in other features in the same branch
					break;
			}					
		}	

		//call recursively for sub features/configs
		if (featureToDel.getFeaturesItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getFeaturesItems()) {
				removeDeletedfromBranchSubItemLists(subItem, season, changedBranches);
			}
		}

		if (featureToDel.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getConfigurationRuleItems()) {
				removeDeletedfromBranchSubItemLists(subItem, season, changedBranches);
			}
		}
		
		if (featureToDel.getEntitlementItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getEntitlementItems()) {
				removeDeletedfromBranchSubItemLists(subItem, season, changedBranches);
			}
		}
		
		if (featureToDel.getPurchaseOptionsItems()!=null) {
			for (BaseAirlockItem subItem:featureToDel.getPurchaseOptionsItems()) {
				removeDeletedfromBranchSubItemLists(subItem, season, changedBranches);
			}
		}
	}

	public static JSONArray roleTypeslistToJsonArray(Set<RoleType> roleTypesList) {
		if (roleTypesList == null)
			return null;
		JSONArray res = new JSONArray();
		for (RoleType rt:roleTypesList) {
			res.add(rt.toString());
		}
		return res;
	}

	public static JSONArray capabilitieslistToJsonArray(Set<AirlockCapability> capabilitiesList) {
		if (capabilitiesList == null)
			return null;
		JSONArray res = new JSONArray();
		for (AirlockCapability rt:capabilitiesList) {
			res.add(rt.toString());
		}
		return res;
	}

	private static void updateNewIdInSubItems(BaseAirlockItem featureInBranch) {
		if(featureInBranch.getFeaturesItems()!=null) {
			for (BaseAirlockItem subItem:featureInBranch.getFeaturesItems()) {
				subItem.setParent(featureInBranch.getUniqueId());
			}
		}

		if(featureInBranch.getConfigurationRuleItems()!=null) {
			for (BaseAirlockItem subItem:featureInBranch.getConfigurationRuleItems()) {
				subItem.setParent(featureInBranch.getUniqueId());
			}
		}
	}

	public static ValidationResults validateBranchStructure(String branchId, Season season, ServletContext context) throws IOException {
		try {
			getAirlockItemsDB(branchId, context);

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch branch = branchesDB.get(branchId);
			MergeBranch.validateBranch(branch);
		} catch (Exception e) {
			//loading seasons branches from s3 since is corrupted on memory
			season.setBranches(new BranchesCollection(season));

			//no need to clean branches db - reading the branches json will override with new object in memeory.

			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String pathSeparator = ds.getSeparator();
			String branchesFileName = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.AIRLOCK_BRANCHES_FILE_NAME;

			try {
				JSONObject seasonBranches = ds.readDataToJSON(branchesFileName);
				Utilities.initFromSeasonBranchesJSON(seasonBranches, context, ds);

			} catch (IOException ioe) {
				String errMsg = String.format(Strings.failedReadingFile,branchesFileName) + ioe.getMessage();
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException je) {
				String errMsg = branchesFileName + " file is not in a legal JSON format: " + je.getMessage();
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}

			return new ValidationResults(Strings.mergeException + e.getMessage(), Status.INTERNAL_SERVER_ERROR);

		}
		return null;
	}


	public static ValidationResults validateBranchPurcahsesStructure(String branchId, Season season, ServletContext context) throws IOException {
		try {
			getAirlockItemsDB(branchId, context);

			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);
			Branch branch = branchesDB.get(branchId);
			MergeBranch.validateBranch(branch);
		} catch (Exception e) {
			//loading seasons branches from s3 since is corrupted on memory
			season.setBranches(new BranchesCollection(season));

			//no need to clean branches db - reading the branches json will override with new object in memeory.

			DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
			String pathSeparator = ds.getSeparator();
			String branchesFileName = Constants.SEASONS_FOLDER_NAME+pathSeparator+season.getProductId().toString()+pathSeparator+season.getUniqueId().toString()+pathSeparator+Constants.AIRLOCK_BRANCHES_FILE_NAME;

			try {
				JSONObject seasonBranches = ds.readDataToJSON(branchesFileName);
				Utilities.initFromSeasonBranchesJSON(seasonBranches, context, ds);

			} catch (IOException ioe) {
				String errMsg = String.format(Strings.failedReadingFile,branchesFileName) + ioe.getMessage();
				logger.severe(errMsg);
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				throw new IOException(errMsg);
			} catch (JSONException je) {
				String errMsg = branchesFileName + " file is not in a legal JSON format: " + je.getMessage();
				logger.severe(errMsg);			
				logger.severe(Strings.changeAirlockSerevrStateTo + "S3_DATA_CONSISTENCY_ERROR.");
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				throw new IOException(errMsg);
			}

			return new ValidationResults(Strings.mergeException + e.getMessage(), Status.INTERNAL_SERVER_ERROR);

		}
		return null;
	}

	public static class LockErrorPair {
		public String error = null;
		public  ReentrantReadWriteLock lock = null;
	}


	public static class ProductErrorPair {
		public String  error = null;
		public Product product = null;
	}
	/*
	//if season is given look at it. Otherwise look for the seasonId in the branch 
	public static LockErrorPair getLockForBranchOrSeason (ServletContext context, String branchId, String seasonId) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		LockErrorPair lockErrorPair = new LockErrorPair(); 

		if (seasonId == null) {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			if (!branchesDB.containsKey(branchId)) {
				lockErrorPair.error = Strings.branchNotFound; 
				logger.severe(lockErrorPair.error);
				return lockErrorPair;
			}

			Branch branch = branchesDB.get(branchId);
			seasonId =  branch.getSeasonId().toString();
		}

		if (!seasonsDB.containsKey(seasonId)) {
			lockErrorPair.error = Strings.seasonNotFound; 
			logger.severe(lockErrorPair.error);
			return lockErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			lockErrorPair.error = Strings.productNotFound;
			logger.severe(lockErrorPair.error);
			return lockErrorPair;
		}

		Product product = productsDB.get(productId);

		lockErrorPair.lock = product.getProductLock();

		return lockErrorPair;				
	}
	 */
	//if season is given look at it. Otherwise look for the seasonId in the branch 
	public static ProductErrorPair getProductOfBranchOrSeason (ServletContext context, String branchId, String seasonId) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair productErrorPair = new ProductErrorPair(); 

		if (seasonId == null) {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			if (!branchesDB.containsKey(branchId)) {
				productErrorPair.error = Strings.branchNotFound; 
				logger.severe(productErrorPair.error);
				return productErrorPair;
			}

			Branch branch = branchesDB.get(branchId);
			seasonId =  branch.getSeasonId().toString();
		}

		if (!seasonsDB.containsKey(seasonId)) {
			productErrorPair.error = Strings.seasonNotFound; 
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			productErrorPair.error = Strings.productNotFound;
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Product product = productsDB.get(productId);

		productErrorPair.product = product;

		return productErrorPair;				
	}

	public static ProductErrorPair getProductOfExperiment (ServletContext context, String experimentId) {
		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair productErrorPair = new ProductErrorPair(); 

		if (!experimentsDB.containsKey(experimentId)) {
			productErrorPair.error = Strings.experimentNotFound; 
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Experiment experiment = experimentsDB.get(experimentId);
		String productId =  experiment.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			productErrorPair.error = Strings.productNotFound; 
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Product product = productsDB.get(productId);

		productErrorPair.product = product;

		return productErrorPair;			
	}

	public static ProductErrorPair getProductOfVariant (ServletContext context, String variantId) {
		@SuppressWarnings("unchecked")
		Map<String, Variant> variantsDB = (Map<String, Variant>)context.getAttribute(Constants.VARIANTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Experiment> experimentsDB = (Map<String, Experiment>)context.getAttribute(Constants.EXPERIMENTS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!variantsDB.containsKey(variantId)) {
			prodErrorPair.error = Strings.variantNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Variant variant = variantsDB.get(variantId);
		String experimentId =  variant.getExperimentId().toString();

		if (!experimentsDB.containsKey(experimentId)) {
			prodErrorPair.error = Strings.experimentNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Experiment experiment = experimentsDB.get(experimentId);
		String productId =  experiment.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;			
	}

	//if season is given look at it. Otherwise look for the seasonId in the branch 
	//items type is Constants.FEATURES_DB_PARAM_NAME or Constants.PURCHASES_DB_PARAM_NAME
	public static ProductErrorPair getProductForBranchOrFeature (ServletContext context, String branchId, String featureId) {

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		String seasonId;
		if (!branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			//look for the seasonId in the branch
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			if (!branchesDB.containsKey(branchId)) {
				prodErrorPair.error = Strings.branchNotFound; 
				logger.severe(prodErrorPair.error);
				return prodErrorPair;
			}

			Branch branch = branchesDB.get(branchId);
			seasonId =  branch.getSeasonId().toString();
		}
		else {
			//for master - look for the seasonId in the feature
			@SuppressWarnings("unchecked")
			Map<String, BaseAirlockItem> airlockItemsDB = (Map<String, BaseAirlockItem>)context.getAttribute(Constants.FEATURES_DB_PARAM_NAME);

			if (!airlockItemsDB.containsKey(featureId)) {
				prodErrorPair.error = Strings.AirlockItemNotFound; 
				logger.severe(prodErrorPair.error);
				return prodErrorPair;
			}

			BaseAirlockItem feature = airlockItemsDB.get(featureId);
			seasonId =  feature.getSeasonId().toString();
		}

		if (!seasonsDB.containsKey(seasonId)) {
			prodErrorPair.error = Strings.seasonNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound;
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;				
	}

	public static ProductErrorPair getProductOfStream(ServletContext context, String streamId) {

		@SuppressWarnings("unchecked")
		Map<String, AirlockStream> streamsDB = (Map<String, AirlockStream>)context.getAttribute(Constants.STREAMS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair productErrorPair = new ProductErrorPair(); 

		if (!streamsDB.containsKey(streamId)) {
			productErrorPair.error = Strings.streamNotFound; 
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		AirlockStream stream = streamsDB.get(streamId);
		String seasonId =  stream.getSeasonId().toString();

		if (!seasonsDB.containsKey(seasonId)) {
			productErrorPair.error = Strings.seasonNotFound; 
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			productErrorPair.error = Strings.productNotFound;
			logger.severe(productErrorPair.error);
			return productErrorPair;
		}

		Product product = productsDB.get(productId);

		productErrorPair.product = product;

		return productErrorPair;				
	}

	public static ProductErrorPair getProductOfNotification(ServletContext context, String notificationId) {

		@SuppressWarnings("unchecked")
		Map<String, AirlockNotification> notificationsDB = (Map<String, AirlockNotification>)context.getAttribute(Constants.NOTIFICATIONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!notificationsDB.containsKey(notificationId)) {
			prodErrorPair.error = Strings.notificationNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		AirlockNotification notification = notificationsDB.get(notificationId);
		String seasonId =  notification.getSeasonId().toString();

		if (!seasonsDB.containsKey(seasonId)) {
			prodErrorPair.error = Strings.seasonNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound;
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;				
	}

	public static ProductErrorPair getProductOfAirlockUser(ServletContext context, String userId) {

		@SuppressWarnings("unchecked")
		Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!usersDB.containsKey(userId)) {
			prodErrorPair.error = Strings.airlockUserNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		UserRoleSet user = usersDB.get(userId);
		UUID productId = user.getProductId();

		//if the user's product is null => this is a global user
		if (productId == null) {
			prodErrorPair.product = null;
		}
		else {
			String productIdStr=  productId.toString();		
			if (!productsDB.containsKey(productIdStr)) {
				prodErrorPair.error = Strings.productNotFound;
				logger.severe(prodErrorPair.error);
				return prodErrorPair;
			}		
			Product product = productsDB.get(productIdStr);			
			prodErrorPair.product = product;
		}
		return prodErrorPair;				
	}

	/*public static LockErrorPair getLockForUtility(ServletContext context, String utilityId) {

		@SuppressWarnings("unchecked")
		Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		LockErrorPair lockErrorPair = new LockErrorPair(); 

		if (!utilitiesDB.containsKey(utilityId)) {
			lockErrorPair.error = Strings.utilityNotFound; 
			logger.severe(lockErrorPair.error);
			return lockErrorPair;
		}

		AirlockUtility utility = utilitiesDB.get(utilityId);
		String seasonId =  utility.getSeasonId().toString();

		if (!seasonsDB.containsKey(seasonId)) {
			lockErrorPair.error = Strings.seasonNotFound; 
			logger.severe(lockErrorPair.error);
			return lockErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			lockErrorPair.error = Strings.productNotFound;
			logger.severe(lockErrorPair.error);
			return lockErrorPair;
		}

		Product product = productsDB.get(productId);

		lockErrorPair.lock = product.getProductLock();

		return lockErrorPair;				
	}*/

	public static ProductErrorPair getProductOfUtility(ServletContext context, String utilityId) {

		@SuppressWarnings("unchecked")
		Map<String, AirlockUtility> utilitiesDB = (Map<String, AirlockUtility>)context.getAttribute(Constants.UTILITIES_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!utilitiesDB.containsKey(utilityId)) {
			prodErrorPair.error = Strings.utilityNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		AirlockUtility utility = utilitiesDB.get(utilityId);
		String seasonId =  utility.getSeasonId().toString();

		if (!seasonsDB.containsKey(seasonId)) {
			prodErrorPair.error = Strings.seasonNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound;
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;				
	}

	public static ProductErrorPair getProductOfString(ServletContext context, String stringId) {

		@SuppressWarnings("unchecked")
		Map<String, OriginalString> originalStringsDB = (Map<String, OriginalString>)context.getAttribute(Constants.ORIG_STRINGS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!originalStringsDB.containsKey(stringId)) {
			prodErrorPair.error = Strings.stringNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		OriginalString originalString = originalStringsDB.get(stringId);
		String seasonId =  originalString.getSeasonId().toString();

		if (!seasonsDB.containsKey(seasonId)) {
			prodErrorPair.error = Strings.seasonNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Season season = seasonsDB.get(seasonId);
		String productId =  season.getProductId().toString();

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound;
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;				
	}

	public static ProductErrorPair getProduct (ServletContext context, String productId) {

		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

		ProductErrorPair prodErrorPair = new ProductErrorPair(); 

		if (!productsDB.containsKey(productId)) {
			prodErrorPair.error = Strings.productNotFound; 
			logger.severe(prodErrorPair.error);
			return prodErrorPair;
		}

		Product product = productsDB.get(productId);

		prodErrorPair.product = product;

		return prodErrorPair;			
	}

	public static void doInitProductsListFromJson(JSONObject productsJSON, Map<String, Product> productsDB,	HashMap<String, Season> seasonsDB, ServletContext context) throws JSONException {
		JSONArray products = productsJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);


		for (int i=0; i<products.size(); i++) {
			JSONObject productJsonObj = products.getJSONObject(i);			
			Product product = new Product();
			product.fromJSON(productJsonObj, seasonsDB, context);

			if (productsDB!=null)
				productsDB.put(product.getUniqueId().toString(), product);
		}	

	}

	public static JSONArray roleTypeslistToJsonArray(Collection<RoleType> roleTypesList) {
		if (roleTypesList == null)
			return null;
		JSONArray res = new JSONArray();
		for (RoleType rt:roleTypesList) {
			res.add(rt.toString());
		}
		return res;
	}
	/*
	public static RoleType strToRoleType(String roleTypeStr) {
		if (roleTypeStr == null) 
			return null;

		if(roleTypeStr.equalsIgnoreCase(RoleType.Viewer.toString())) 
			return RoleType.Viewer;

		if(roleTypeStr.equalsIgnoreCase(RoleType.Administrator.toString())) 
			return RoleType.Administrator;

		if(roleTypeStr.equalsIgnoreCase(RoleType.Editor.toString())) 
			return RoleType.Editor;

		if(roleTypeStr.equalsIgnoreCase(RoleType.AnalyticsEditor.toString())) 
			return RoleType.AnalyticsEditor;

		if(roleTypeStr.equalsIgnoreCase(RoleType.AnalyticsViewer.toString())) 
			return RoleType.AnalyticsViewer;

		if(roleTypeStr.equalsIgnoreCase(RoleType.ProductLead.toString())) 
			return RoleType.ProductLead;

		if(roleTypeStr.equalsIgnoreCase(RoleType.TranslationSpecialist.toString())) 
			return RoleType.TranslationSpecialist;


		return null; 
	}
	 */

	public static void initAirlockKeysPasswords(JSONObject keysJSON, JSONObject keysPasswordsJSON, ServletContext context) throws JSONException {
		AirlockAPIKeys apiKeys = (AirlockAPIKeys)context.getAttribute(Constants.API_KEYS_PARAM_NAME);
		apiKeys.fromJSON(keysJSON, keysPasswordsJSON);				
	}

	public static String generateAirlockKeyPassword()
	{
		Random rand = new Random();
		int num1 = rand.nextInt();
		int num2 = rand.nextInt();
		byte[] array = ByteBuffer.allocate(8).putInt(num1).putInt(num2).array();
		return Base64.encodeBase64String(array);
	}

	public static ValidationResults validateCapability(Product product, AirlockCapability[] capabilities) {
		for (AirlockCapability capability:capabilities) {
			if (!product.getCapabilities().contains(capability)) {
				String errMsg = String.format(Strings.productDoesNotEnableCapability, capability.toString(), product.getName());
				logger.severe(errMsg);
				return new ValidationResults(errMsg, Status.BAD_REQUEST);
			}
		}
		return null;
	}
	/*
	public static JSONObject readResetElasticSearchScript(ServletContext context) {
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		if (ds instanceof S3DataSerializer) { //only s3 serialization supports encryption
			String resetElasticScriptFileName = Constants.RESET_ELASTIC_SCRIPT_FILE_NAME;

			try {
				logger.info ("reading  " + resetElasticScriptFileName);
				JSONObject res = new JSONObject(((S3DataSerializer)ds).getEncryptedFileShortName(resetElasticScriptFileName));
				//((S3DataSerializer)ds).putEncryptedFile(o.write(true), resetElasticScriptFileName);
				return res;
			} catch (IOException e) {
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				String error = String.format(Strings.failedInitializationReadingFile, resetElasticScriptFileName) + e.getMessage();
				logger.severe(error);
				logger.severe("Changing Airlock service state to S3_IO_ERROR.");
				throw new RuntimeException(error);
			} catch (JSONException e) {
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_DATA_CONSISTENCY_ERROR);
				String errMsg = String.format(Strings.failedInitializationInvalidJson, resetElasticScriptFileName) + e.getMessage();
				logger.severe(errMsg);		
				logger.severe("Changing Airlock service state to S3_DATA_CONSISTENCY_ERROR.");
				throw new RuntimeException(errMsg);
			} catch (Exception e) {
				context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
				String error = String.format(Strings.failedInitializationReadingFile, resetElasticScriptFileName) + e.getMessage();
				logger.severe(error);
				logger.severe("Changing Airlock service state to S3_IO_ERROR.");
				throw new RuntimeException(error);
			}			
		}

		return null;
	}

	public static void resetElasticSearch(ServletContext context) {
		JSONObject resetElasticScriptJson = readResetElasticSearchScript(context);
		if ( resetElasticScriptJson == null) { //not S3DataSerializer - do nothing
			logger.info("resetElasticSearch request for other than s3 data serializer - do nothing");
			return;
		}

	}*/

	public static File generateRuntimeDefaultsZip(Season season, ServletContext context, List<String> locales) throws Exception{
		String separator = "/";
		//the folder to copy all runtime files to
		String sourceFolderPath ="temp"+separator+season.getUniqueId().toString();
		String zipFilePath = "temp"+separator+Constants.RUNTIME_DEFAULTS_ZIP_FILE_NAME;

		//delete left overs
		FileUtils.deleteDirectory(new File(sourceFolderPath));

		File sourceFolder = new File(sourceFolderPath);

		if(!sourceFolder.exists())
			sourceFolder.mkdirs();

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);

		copyRuntimeFileStringContent(ds, Constants.AIRLOCK_RUNTIME_DEVELOPMENT_FILE_NAME, sourceFolderPath, season, context);
		copyRuntimeFileStringContent(ds, Constants.AIRLOCK_UTILITIES_DEVELOPMENT_FILE_NAME, sourceFolderPath, season, context);
		copyRuntimeFileStringContent(ds, Constants.AIRLOCK_STREAMS_DEVELOPMENT_FILE_NAME, sourceFolderPath, season, context);
		copyRuntimeFileStringContent(ds, Constants.AIRLOCK_NOTIFICATIONS_DEVELOPMENT_FILE_NAME, sourceFolderPath, season, context);
		copyRuntimeFileStringContent(ds, Constants.AIRLOCK_STREAMS_UTILITIES_DEVELOPMENT_FILE_NAME, sourceFolderPath, season, context);
		//TODO: add translations
		if (locales == null) {
			// copy all locals
			List<String> existingLocales = TranslationUtilities.getLocalesByStringsFiles(season.getProductId().toString(), season.getUniqueId().toString(), ds, logger);
			copyTranslationLocaleStringContent(ds, "original", sourceFolderPath, season, context);
			for (String locale : existingLocales) {
				copyTranslationLocaleStringContent(ds, locale, sourceFolderPath, season, context);
			}
		} else {
			for (String locale : locales) {
				copyTranslationLocaleStringContent(ds, locale, sourceFolderPath, season, context);
			}
		}
		zip(zipFilePath,sourceFolderPath);
		return new File(zipFilePath);

	}
	private static void copyTranslationLocaleStringContent(DataSerializer ds, String locale, String destinationFolderPath, Season season, ServletContext context) throws IOException {
		String repSeperator = ds.getSeparator();
		String season_id = season.getUniqueId().toString();
		String product_id = season.getProductId().toString();
		String translationsFolderPath = Constants.SEASONS_FOLDER_NAME+repSeperator+product_id.toString()+
				repSeperator+season_id+repSeperator+Constants.TRANSLATIONS_FOLDER_NAME;
		//the file name format is: strings__<locale>.json
		String fileName = "strings__" + locale + ".json";
		if (locale.equals("original")) {
			fileName = Constants.ORIGINAL_STRINGS_FILE_NAME;
		}
		String localeFilePath = translationsFolderPath + repSeperator + "strings__" + locale;

		String data = null;
		try {
			data = ds.readDataToString(localeFilePath);
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);
		} 

		File runtimeProdFile = new File (destinationFolderPath, fileName);

		OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(runtimeProdFile));
		os.write(data);
		os.flush();
		os.close();
	}
	private static void copyRuntimeFileStringContent(DataSerializer ds, String fileName, String destinationFolderPath, Season season, ServletContext context) throws IOException, JSONException{
		String repSeperator = ds.getSeparator();
		String filePath = Constants.SEASONS_FOLDER_NAME+repSeperator+season.getProductId().toString()+repSeperator+season.getUniqueId().toString()+repSeperator+fileName;

		String data = null;
		try {
			data = ds.readDataToString(filePath);
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);
		} 

		File runtimeProdFile = new File (destinationFolderPath, fileName);

		OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(runtimeProdFile));
		os.write(data);
		os.flush();
		os.close();

	}

	//TODO: taken from ImportExportUtilities - can move all to utilities 
	public static void zip(String zipFilePath,String sourceFolder) throws Exception{
		byte[] buffer = new byte[1024];
		String source = new File(sourceFolder).getName();
		File zipFile = new File(zipFilePath);
		if(zipFile.exists()){
			zipFile.delete();
		}
		FileOutputStream fos = new FileOutputStream(zipFilePath);
		ZipOutputStream zos = new ZipOutputStream(fos);
		logger.info("Zipping folder: "+sourceFolder+ "to zipFile: "+zipFilePath);
		FileInputStream in = null;
		File sourceDirectory = new File(sourceFolder);
		List <String> fileList = new ArrayList<>();
		generateFileList(sourceDirectory,sourceFolder,fileList);
		for (String file: fileList) {
			logger.info("File Added : " + file);
			ZipEntry ze = new ZipEntry(source + File.separator + file);
			zos.putNextEntry(ze);
			try {
				in = new FileInputStream(sourceFolder + File.separator + file);
				int len;
				while ((len = in .read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
			} finally {
				if(in != null)
					in.close();
			}
		}
		zos.closeEntry();
		zos.close();
		FileUtils.deleteDirectory(new File(sourceFolder));
		logger.info("Folder successfully compressed");

	}

	private static void generateFileList(File node, String sourceFolder,List <String> fileList) {
		// add file only
		if (node.isFile()) {
			fileList.add(generateZipEntry(node.toString(),sourceFolder));
		}

		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename: subNote) {
				generateFileList(new File(node, filename),sourceFolder,fileList);
			}
		}
	}

	private static String generateZipEntry(String file, String sourceFolder) {
		return file.substring(sourceFolder.length() + 1, file.length());
	}

	public static JSONObject buildRoleSetsListForUser(ServletContext context, String userIdentifier) throws JSONException {
		userIdentifier = userIdentifier.toLowerCase();
		JSONObject res = new JSONObject();
		JSONArray roleSetsArr = new JSONArray();

		@SuppressWarnings("unchecked")
		Map<String, UserRoleSet> usersDB = (Map<String, UserRoleSet>)context.getAttribute(Constants.AIRLOCK_USERS_DB_PARAM_NAME);

		Set<String> usersIds = usersDB.keySet();
		for (String userId:usersIds) {
			UserRoleSet urs = usersDB.get(userId);
			if (urs.getUserIdentifier().toLowerCase().equals(userIdentifier)) {
				roleSetsArr.add(urs.toJSON());
			}
		}
		res.put(Constants.JSON_FIELD_ROLE_SETS, roleSetsArr);
		return res;
	}

	public static String listProductsInWhichUserHasPermissions(ServletContext context, String identifier) {
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);

		StringBuilder sb = new StringBuilder();
		Set<String> productIds = productsDB.keySet();
		for (String prodId:productIds) {
			Product prod = productsDB.get(prodId);
			if (prod.getProductUsers().getUserByIdentifier(identifier) !=null) {
				if (sb.length()!=0)
					sb.append(", ");
				sb.append(prod.getName());
			}
		}
		return sb.toString();
	}


	//An Administrator is also a ProductLead, Editor and Viewer
	//A ProductLead is also Editor and Viewer
	//An Editor is also a viewer
	//A TranslationSpecialist is also a viewer
	//An AnalyticsViewer is also a viewer
	//An AnalyticsEditor is also a viewer
	public static LinkedHashSet<String> setRolesListByHigherPermission(JSONArray existingRolesArray) throws JSONException {
		//this function is called after validate so we know that the roles list is valid (no duplications, existing roles)
		LinkedHashSet<String> newRolesSet = new LinkedHashSet<String>();
		for (int i=0; i<existingRolesArray.size(); i++) {
			RoleType existingRole = Utilities.strToRoleType(existingRolesArray.getString(i));
			switch (existingRole) {
			case Administrator: 
				newRolesSet.add(RoleType.Administrator.toString());
				newRolesSet.add(RoleType.ProductLead.toString());
				newRolesSet.add(RoleType.Editor.toString());
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case ProductLead:
				newRolesSet.add(RoleType.ProductLead.toString());						
				newRolesSet.add(RoleType.Editor.toString());
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case Editor:
				newRolesSet.add(RoleType.Editor.toString());
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case TranslationSpecialist:
				newRolesSet.add(RoleType.TranslationSpecialist.toString());						
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case AnalyticsViewer:
				newRolesSet.add(RoleType.AnalyticsViewer.toString());
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case AnalyticsEditor:
				newRolesSet.add(RoleType.AnalyticsEditor.toString());
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			case Viewer:
				newRolesSet.add(RoleType.Viewer.toString());
				break;
			}
		}
		return newRolesSet;
	}

	public static boolean isServerSupportRuntimeEncryption(ServletContext context) {
		AirlockCapabilities capabilities = (AirlockCapabilities)context.getAttribute(Constants.CAPABILITIES_PARAM_NAME);

		if (capabilities.getCapabilities().contains(AirlockCapability.RUNTIME_ENCRYPTION)) {
			return true;
		}

		return false;

	}

	public static Map<String, BaseAirlockItem> getPurchaseItemsMapCopyForSeason (ServletContext context, String seasonId) throws IOException {
		@SuppressWarnings("unchecked")			
		Map<String, Season> seasonsDB = (Map<String, Season>)context.getAttribute(Constants.SEASONS_DB_PARAM_NAME);

		Map<String, BaseAirlockItem> copyItemsMap = new ConcurrentHashMap<String, BaseAirlockItem>();
		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String pathSeparator = ds.getSeparator();

		String seasonPurchasesFile = Constants.SEASONS_FOLDER_NAME + pathSeparator + seasonsDB.get(seasonId).getProductId().toString() 
				+ pathSeparator + seasonId+pathSeparator + Constants.AIRLOCK_ENTITLEMENTS_FILE_NAME;
		try {
			JSONObject purchasesJSON = ds.readDataToJSON(seasonPurchasesFile);
			initFromSeasonFeaturesJSON(purchasesJSON, seasonsDB, copyItemsMap, ds, false, Constants.REQUEST_ITEM_TYPE.ENTITLEMENTS); //only create new purchases db - don't update season
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,"the defaults") + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		} catch (JSONException e) {
			String errMsg = seasonPurchasesFile + " file is not in a legal JSON format: " + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);		
		}			

		return copyItemsMap;
	}

	public static String featuresAttachedToEntitlement(BaseAirlockItem purchaseItem, Season season, String branchId, ServletContext context) {
		if (!purchaseItem.getType().equals(Type.ENTITLEMENT) && !purchaseItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) ) {
			return null; //only ENTITLEMENT items are referred from features  
		}

		String res = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			//look in master and in all branches in season
			RootItem masterRoot = season.getRoot();
			res = isPurchaseInFeature(masterRoot, purchaseItem.getUniqueId().toString(), Constants.MASTER_BRANCH_NAME, false);
			if (res!=null)
				return res;

			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().get(purchaseItem.getUniqueId().toString())!=null) {
					continue; //if the purchase is checked out in the branch - it can be deleted from master
				}
				for (BaseAirlockItem alItem:branch.getBranchFeatures()) {
					res = isPurchaseInFeature(alItem, purchaseItem.getUniqueId().toString(), branch.getName(), false);
					if (res!=null)
						return res;
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch currentBranch = branchesDB.get(branchId);
			if (currentBranch!=null) {
				for (BaseAirlockItem alItem:currentBranch.getBranchFeatures()) {
					res = isPurchaseInFeature(alItem, purchaseItem.getUniqueId().toString(), currentBranch.getName(), false);
					if (res!=null)
						return res;
				}
			}
		}

		return res;
	}

	public static String productionFeaturesAttachedToEntitlement(BaseAirlockItem purchaseItem, Season season, String branchId, ServletContext context) {
		if (!purchaseItem.getType().equals(Type.ENTITLEMENT) && !purchaseItem.getType().equals(Type.ENTITLEMENT_MUTUAL_EXCLUSION_GROUP) ) {
			return null; //only ENTITLEMENT items are referred from features  
		}

		String res = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			//look in master and in all branches in season
			RootItem masterRoot = season.getRoot();
			res = isPurchaseInFeature(masterRoot, purchaseItem.getUniqueId().toString(), Constants.MASTER_BRANCH_NAME, true);
			if (res!=null)
				return res;

			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().get(purchaseItem.getUniqueId().toString())!=null) {
					continue; //if the purchase is checked out in the branch - it can be deleted from master
				}
				for (BaseAirlockItem alItem:branch.getBranchFeatures()) {
					res = isPurchaseInFeature(alItem, purchaseItem.getUniqueId().toString(), branch.getName(), true);
					if (res!=null)
						return res;
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch currentBranch = branchesDB.get(branchId);
			if (currentBranch!=null) {
				for (BaseAirlockItem alItem:currentBranch.getBranchFeatures()) {
					res = isPurchaseInFeature(alItem, purchaseItem.getUniqueId().toString(), currentBranch.getName(), true);
					if (res!=null)
						return res;
				}
			}
		}

		return res;
	}

	private static String isPurchaseInFeature(BaseAirlockItem root, String purchaseId, String branchName, boolean onlyProduction) {
		if (root.getType().equals(Type.FEATURE)) {
			FeatureItem fi = (FeatureItem) root;
			if (!onlyProduction || fi.getStage().equals(Stage.PRODUCTION)) {
				if (fi.getPremium() && fi.getEntitlement()!=null && fi.getEntitlement().equals(purchaseId)) {
					return String.format(Strings.purchaseAttachedToFeature, fi.getNameSpaceDotName(), branchName);
				}
			}
		}
		if (root.getFeaturesItems()!=null && !root.getFeaturesItems().isEmpty()) {
			for (int i=0;i<root.getFeaturesItems().size(); ++i) {
				String res = isPurchaseInFeature(root.getFeaturesItems().get(i), purchaseId, branchName, onlyProduction);
				if (res!=null) {
					return res;
				}
			}
		}
		return null;
	}

	//TODO: handle in branches
	public static String deletedPurchaseIsIncludedInOtherPurcahse(BaseAirlockItem purchaseItem, Season season, String branchId, ServletContext context) {
		if (!purchaseItem.getType().equals(Type.ENTITLEMENT)) {
			return null; //only ENTITLEMENT items are referred from features  
		}

		String res = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			//look in master and in all branches in season
			RootItem masterRoot = season.getEntitlementsRoot();
			res = isPurchaseIncludedInPurcahse(masterRoot, purchaseItem.getUniqueId().toString(), Constants.MASTER_BRANCH_NAME);
			if (res!=null)
				return res;

			for (Branch branch:season.getBranches().getBranchesList()) {
				if (branch.getBranchAirlockItemsBD().get(purchaseItem.getUniqueId().toString())!=null) {
					continue; //if the deleted purchase is checked out in the branch - it can be deleted from master
				}
				for (BaseAirlockItem alItem:branch.getBranchPurchases()) {
					res = isPurchaseIncludedInPurcahse(alItem, purchaseItem.getUniqueId().toString(), branch.getName());
					if (res!=null)
						return res;
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch currentBranch = branchesDB.get(branchId);
			if (currentBranch!=null) {
				for (BaseAirlockItem alItem:currentBranch.getBranchPurchases()) {
					res = isPurchaseIncludedInPurcahse(alItem, purchaseItem.getUniqueId().toString(), currentBranch.getName());
					if (res!=null)
						return res;
				}
			}
		}

		return res;
	}

	private static String isPurchaseIncludedInPurcahse(BaseAirlockItem root, String purchaseId, String branchName) {
		if (root.getType().equals(Type.ENTITLEMENT)) {
			EntitlementItem iap = (EntitlementItem) root;
			if (iap.getIncludedPurchases()!=null && isStringInList(iap.getIncludedPurchases(), purchaseId)) {
				return String.format(Strings.entitlementIncludedInOtherEntitlements, iap.getNameSpaceDotName(), branchName);
			}
		}
		if (root.getEntitlementItems()!=null && !root.getEntitlementItems().isEmpty()) {
			for (int i=0;i<root.getEntitlementItems().size(); ++i) {
				String res = isPurchaseIncludedInPurcahse(root.getEntitlementItems().get(i), purchaseId, branchName);
				if (res!=null) {
					return res;
				}
			}
		}
		return null;
	}

	private static boolean isStringInList(LinkedList<String> list, String str) {
		if (list == null || list.isEmpty())
			return false;

		for (String s:list) {
			if (s.equals(str))
				return true;

		}

		return false;
	}
	
	public static String prodFeaturesAttachedToPurchase(BaseAirlockItem purchaseItem, Season season, String branchId, ServletContext context) {
		if (!purchaseItem.getType().equals(Type.ENTITLEMENT)) {
			return null; //only ENTITLEMENT items are referred from features  
		}

		String res = null;
		if (branchId.equals(Constants.MASTER_BRANCH_NAME)) {
			//look in master and in all branches in season
			RootItem masterRoot = season.getRoot();
			res = isPurchaseInProdFeature(masterRoot, purchaseItem.getUniqueId().toString(), Constants.MASTER_BRANCH_NAME);
			if (res!=null)
				return res;

			for (Branch branch:season.getBranches().getBranchesList()) {
				for (BaseAirlockItem alItem:branch.getBranchFeatures()) {
					res = isPurchaseInProdFeature(alItem, purchaseItem.getUniqueId().toString(), branch.getName());
					if (res!=null)
						return res;
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Branch> branchesDB = (Map<String, Branch>)context.getAttribute(Constants.BRANCHES_DB_PARAM_NAME);

			Branch currentBranch = branchesDB.get(branchId);
			if (currentBranch!=null) {
				for (BaseAirlockItem alItem:currentBranch.getBranchFeatures()) {
					res = isPurchaseInProdFeature(alItem, purchaseItem.getUniqueId().toString(), currentBranch.getName());
					if (res!=null)
						return res;
				}
			}
		}

		return res;
	}

	private static String isPurchaseInProdFeature(BaseAirlockItem root, String purchaseId, String branchName) {
		if (root.getType().equals(Type.FEATURE)) {
			FeatureItem fi = (FeatureItem) root;
			if (fi.getPremium() && fi.getEntitlement()!=null && fi.getEntitlement().equals(purchaseId) && fi.getStage().equals(Stage.PRODUCTION)) {
				return String.format(Strings.entitlementAttachedToProdFeature, fi.getNameSpaceDotName(), branchName);
			}
		}
		if (root.getFeaturesItems()!=null && !root.getFeaturesItems().isEmpty()) {
			for (int i=0;i<root.getFeaturesItems().size(); ++i) {
				String res = isPurchaseInProdFeature(root.getFeaturesItems().get(i), purchaseId, branchName);
				if (res!=null) {
					return res;
				}
			}
		}
		return null;
	}
	public static String deletedPurchaseOptionsCauseEntitlementWithoutStoreId(BaseAirlockItem deletedPurchaseOptionsItem,
			Season season, String branchId, ServletContext context, Map<String, BaseAirlockItem> airlockItemsDB) {
		if (!deletedPurchaseOptionsItem.getType().equals(Type.PURCHASE_OPTIONS) && !deletedPurchaseOptionsItem.getType().equals(Type.PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP)) {
			return null; //only ENTITLEMENT items are referred from features  
		}
		
		EntitlementItem entitlement = deletedPurchaseOptionsItem.getParentEntitlement(airlockItemsDB);
		String attachedFeatures = Utilities.featuresAttachedToEntitlement(entitlement, season, branchId, context);
		if (attachedFeatures!=null) {
			//the parent entitlement is attached to premium feature - validate that the purchaseOptions deletion
			//doesn't leave the entitlement without any storeProductId
			if (!entitlement.hasPurchaseOptionsWithProductId(deletedPurchaseOptionsItem.getUniqueId(), null, null)) {
				return Strings.cannotDeletePurchaseOptionsLeavesEntitlementWithoutStoreProdId;
			}
		}
		
		return null;

	}

	
}
