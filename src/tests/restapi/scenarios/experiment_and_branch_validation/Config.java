//package tests.restapi.scenarios.experiment_and_branch_validation;
package tests.restapi.scenarios.experiment_and_branch_validation;

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
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);
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

	String updateSchema(String path) throws Exception
	{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + path, "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        return schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	}
	String addExperiment(String minVersion, String maxVersion, String path, String rule, boolean enabled) throws Exception
	{
		String experiment = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("stage", stage);
		expJson.put("minVersion", minVersion);
		expJson.put("maxVersion", maxVersion);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		if (rule != null)
		{
			JSONObject json = new JSONObject();
			json.put("ruleString", rule);
			expJson.put("rule", json);
		}
		
		expJson.put("enabled", enabled);
		
		return exp.createExperiment(productID, expJson.toString(), sessionToken);
	}
	String addVariant(String experimentID, String variantName, String branchName, String path, String rule) throws Exception
	{
		String variant = FileUtils.fileToString(filePath + path, "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", stage);
		if (rule != null)
		{
			JSONObject json = new JSONObject();
			json.put("ruleString", rule);
			variantJson.put("rule", json);
		}
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);
	}
	String addUtility(String minVer, String content) throws Exception, Exception
	{
		Properties prop = new Properties();
		prop.setProperty("utility", content);
		prop.setProperty("minAppVersion", minVer);
		prop.setProperty("stage", stage);
		return u.addUtility(seasonID, prop, sessionToken);
	}
	String updateUtility(String utilityId, String content) throws Exception
	{
		String utility = u.getUtility(utilityId, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", content);
		
		//JSONObject json = new JSONObject(content);
		return u.updateUtility(utilityId, uJson, sessionToken);
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
		
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		return stringsApi.addString(seasonID, str, sessionToken);
	}
	
	void reset()
	{
		baseUtils.reset(productID, sessionToken);
	}
}
