package tests.restapi.scenarios.orderingRules;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
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
		f = new FeaturesRestApi();
		f.setURL(url);
		br = new BranchesRestApi();
		br.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);
		baseUtils = new AirlockUtils(url, configPath, sessionToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}

	 String addSeason(String minVer) throws Exception
	{
		JSONObject season = new JSONObject();
		season.put("minVersion", minVer);
		seasonID = s.addSeason(productID, season.toString(), sessionToken);
		return seasonID;
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
	String addBranchFeature(String branchID, String minVersion, String path, String rule) throws Exception
	{
		String feature = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		if (rule != null)
		{
			JSONObject jj = new JSONObject();
			jj.put("ruleString", rule);
			json.put("rule", jj);
		}
		return f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
	}
	
	String addFeature(String path, String rule, String parent, String minVersion) throws Exception
	{
		String feature = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		if (rule != null)
		{
			JSONObject jj = new JSONObject();
			jj.put("ruleString", rule);
			json.put("rule", jj);
		}
		return f.addFeature(seasonID, json.toString(), parent, sessionToken);
	}
	
	String addFeatureWithString(String path, String stringKey, String stringParam, String parent, String minVersion) throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		String configuration =  "{ \"text\" :  translate(\"" + stringKey + "\", \"" + stringParam + "\")	}" ;		
		json.put("configuration", configuration);
		return f.addFeature(seasonID, json.toString(), parent, sessionToken);
	}
	
	String addMTX(String path, String parent) throws Exception
	{
		String feature = FileUtils.fileToString(filePath + path, "UTF-8", false);
		return f.addFeature(seasonID, feature.toString(), parent, sessionToken);
	}
	
	String updateFeature(String featureId, String rule) throws Exception
	{
		String feature = f.getFeature(featureId, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", stage);
		if (rule != null)
		{
			JSONObject jj = new JSONObject();
			jj.put("ruleString", rule);
			json.put("rule", jj);
		}
		return f.updateFeature(seasonID, featureId, json.toString(), sessionToken);
	}
	
	String updateFeatureField(String featureId, String field, String value) throws Exception
	{
		String feature = f.getFeature(featureId, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put(field, value);
		return f.updateFeature(seasonID, featureId, json.toString(), sessionToken);
	}
	
	String updateFeatureWithMinVersion(String featureId, String minVersion) throws Exception
	{
		String feature = f.getFeature(featureId, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		return f.updateFeature(seasonID, featureId, json.toString(), sessionToken);
	}
	

	String addFeatureWithConfiguration(String path, String rule, String parent, String minVersion, String configuration) throws Exception
	{
		String feature = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", stage);
		json.put("minAppVersion", minVersion);
		json.put("configuration", configuration);
		if (rule != null)
		{
			JSONObject jj = new JSONObject();
			jj.put("ruleString", rule);
			json.put("rule", jj);
		}
		return f.addFeature(seasonID, json.toString(), parent, sessionToken);
	}
	
	

	String updateSchema(String path) throws Exception
	{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + path, "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        return schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	}

	String addUtility(String content) throws Exception, Exception
	{
		Properties prop = new Properties();
		prop.setProperty("utility", content);
		//prop.setProperty("minAppVersion", minVer);
		prop.setProperty("stage", stage);
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
	
	String getFeature(String featureId)
	{
		return f.getFeature(featureId, sessionToken);
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
	
	int deleteFeature(String featureId){
		return f.deleteFeature(featureId, sessionToken);
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
	
	void reset()
	{
		baseUtils.reset(productID, sessionToken);
	}
}
