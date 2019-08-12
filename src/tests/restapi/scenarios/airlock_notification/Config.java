package tests.restapi.scenarios.airlock_notification;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;


public class Config
{
	String seasonID, productID;
	String filePath;
	String url;
	ProductsRestApi p;
	FeaturesRestApi f;
	String sessionToken = "";
	AirlockUtils baseUtils;
	BranchesRestApi br ;
	UtilitiesRestApi u;
	SeasonsRestApi s;
	InputSchemaRestApi schema;
	ExperimentsRestApi exp;
	AirlocklNotificationRestApi notifApi;
	StringsRestApi stringsApi;
	String stage = "DEVELOPMENT";
	StreamsRestApi streamApi;

	Config(String inUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		url = inUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		br = new BranchesRestApi();
		br.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);
		notifApi = new AirlocklNotificationRestApi();
		notifApi.setUrl(url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		baseUtils = new AirlockUtils(url, configPath, sessionToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}

	void addSeason(String minVer) throws Exception
	{
		JSONObject season = new JSONObject();
		season.put("minVersion", minVer);
		seasonID = s.addSeason(productID, season.toString(), sessionToken);
	}
	void createSchema() throws Exception
	{
		baseUtils.createSchema(seasonID);
	}
	String addBranch(String branchName, String path) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	String updateSchema(String path) throws Exception
	{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + path, "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        return schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	}

	String addUtility(String minVer, String content) throws Exception, Exception
	{
		Properties prop = new Properties();
		prop.setProperty("utility", content);
		prop.setProperty("minAppVersion", minVer);
		prop.setProperty("stage", stage);
		prop.setProperty("name", RandomStringUtils.randomAlphabetic(5));
		return u.addUtility(seasonID, prop, sessionToken);
	}
	String updateUtility(String utilityId, String content) throws Exception
	{
		JSONObject json = new JSONObject(content);
		return u.updateUtility(utilityId, json, sessionToken);
	}
	int deleteUtility(String utilityID)
	{
		return u.deleteUtility(utilityID, sessionToken);
	}
	String getUtility(String utilityID)
	{
		return u.getUtility(utilityID, sessionToken);
	}
	
	String addString(String path) throws Exception{
		
		String str = FileUtils.fileToString(filePath + path, "UTF-8", false);
		return stringsApi.addString(seasonID, str, sessionToken);
	}
	
	String updateString(String stringId, String newStage) throws Exception{
		String str = stringsApi.getString(stringId, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("stage", newStage);
		return stringsApi.updateString(stringId, json.toString(), sessionToken);
	}
	
	int deleteString (String stringId) throws Exception{
		return stringsApi.deleteString(stringId, sessionToken);
	}
	
	String 	createStream(String minAppVersion) throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamJson.put("minAppVersion", minAppVersion);
		return  streamApi.createStream(seasonID, streamJson.toString(), sessionToken);

	}
	
	int deleteStream(String streamID) throws Exception{		
		return streamApi.deleteStream(streamID, sessionToken);
	}
	
	String updateStreamRemoveField(String streamID) throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema2.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		return streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		
	}
	
	String updateStream(String streamID, String field, String value) throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put(field, value);
		return streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		
	}
	
	//********************
	
	String addNotification(String fileName, String field, String rule, String minVersion) throws IOException, JSONException{
		String notification = FileUtils.fileToString(filePath + "notifications/" + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);

			if (rule != null)
			{
				JSONObject jj = new JSONObject();
				jj.put("ruleString", rule);
				json.put(field, jj);
			}
			
		return notifApi.createNotification(seasonID, json.toString(), sessionToken);
	}
	
	String addNotificationWithConfiguration(String[] requiredFields, String fileName, String field, String rule, String minVersion, boolean contextField) throws IOException, JSONException{
		String notification = FileUtils.fileToString(filePath + "notifications/" + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);

			String configuration = setConfiguration(requiredFields, rule, contextField);
			json.put("configuration", configuration);	
		return notifApi.createNotification(seasonID, json.toString(), sessionToken);
	}
	
	String updateNotificationField(String id, String field, String value) throws JSONException{
		String notification = notifApi.getNotification(id, sessionToken);
		JSONObject json = new JSONObject(notification);
		json.put(field, value);
		return notifApi.updateNotification(id, json.toString(), sessionToken);
	}

	
	
	String addNotificationWithString(String fileName, String stringKey, String stringParam, String minVersion) throws IOException, JSONException{
		String notification = FileUtils.fileToString(filePath + "notifications/" + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		String configuration =  setConfigurationWithUtility("title", "translate(\"" + stringKey + "\", \"" + stringParam + "\")") ;			
		json.put("configuration", configuration);

		return notifApi.createNotification(seasonID, json.toString(), sessionToken);
	}
	
	String udpateNotificationWithString(String id, String stringKey, String stringParam) throws IOException, JSONException{
		String notification = notifApi.getNotification(id, sessionToken);
		JSONObject json = new JSONObject(notification);
		String configuration =  setConfigurationWithUtility("title", "translate(\"" + stringKey + "\", \"" + stringParam + "\")") ;		
		json.put("configuration", configuration);


		return notifApi.updateNotification(id, json.toString(), sessionToken);
	}
	
	String updateNotificationRuleField(String id, String field, String value) throws JSONException, IOException{
		String notification = notifApi.getNotification(id, sessionToken);
		JSONObject json = new JSONObject(notification);

			JSONObject jj = new JSONObject();
			jj.put("ruleString", value);
			json.put(field, jj);
			
		return notifApi.updateNotification(id, json.toString(), sessionToken);
		
	}
	
	String updateNotificationWithConfiguration(String[] requiredFields, String id, String field, String value, boolean contextField) throws JSONException, IOException{
		String notification = notifApi.getNotification(id, sessionToken);
		JSONObject json = new JSONObject(notification);

			String configuration = setConfiguration(requiredFields, value, contextField);
			json.put("configuration", configuration);
	
		return notifApi.updateNotification(id, json.toString(), sessionToken);
		
	}

	int deleteNotification(String id){
		return notifApi.deleteNotification(id, sessionToken);
	}
	
	void reset()
	{
		baseUtils.reset(productID, sessionToken);
	}
	
/*	String setConfiguration(String field, String value, boolean contextField) throws IOException, JSONException{
		String schema = FileUtils.fileToString(filePath + "notifications/notificationSchema1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(schema);
		JSONArray requiredFields = json.getJSONArray("required");
		String configuration = "{\"notification\":";

		value = value.replaceAll("\"", "'");
		
		for (int i=0; i< requiredFields.size(); i++) {
		    String key = (String)requiredFields.get(i);
		    if (contextField)
		    	configuration = configuration + "{\"" + key + "\": " + value + "},";
		    else 
		    	configuration = configuration + "{\"" + key + "\": \"" + value + "\"},";
		}
		
		configuration = configuration.substring(0, configuration.length() - 1);	//remove last ,
		configuration += "}";
		return configuration;
	}
*/
	
	String setConfiguration(String[] requiredFields, String value, boolean contextField) throws IOException, JSONException{
		String schema = FileUtils.fileToString(filePath + "notifications/notificationSchema1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(schema);
		String configuration = "{\"notification\":{";

		value = value.replaceAll("\"", "'");
		
		for (int i=0; i< requiredFields.length; i++) {
		    String key = (String)requiredFields[i];
		    if (contextField)
		    	configuration = configuration + "\"" + key + "\": " + value + ",";
		    else 
		    	configuration = configuration + "\"" + key + "\": \"" + value + "\",";
		}
		
		configuration = configuration.substring(0, configuration.length() - 1);	//remove last ,
		configuration += "}}";
		return configuration;
	}
	
	String addNotificationWithUtility(String fileName, String field, String rule, String minVersion) throws IOException, JSONException{
		String notification = FileUtils.fileToString(filePath + "notifications/" + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		String configuration = setConfigurationWithUtility("title", rule);
		json.put("configuration", configuration);
		return notifApi.createNotification(seasonID, json.toString(), sessionToken);
	}
	
	String updateNotificationWithUtility(String id, String field, String value) throws JSONException, IOException{
		String notification = notifApi.getNotification(id, sessionToken);
		JSONObject json = new JSONObject(notification);
		String configuration = setConfigurationWithUtility("title", value);
		json.put("configuration", configuration);	
		return notifApi.updateNotification(id, json.toString(), sessionToken);
		
	}
	
	String setConfigurationWithUtility(String field, String value) throws IOException, JSONException{

		return "{\"notification\": {\"" + field +  "\": " + value + "}}";

	}
}
