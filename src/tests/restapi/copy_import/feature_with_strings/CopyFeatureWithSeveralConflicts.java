package tests.restapi.copy_import.feature_with_strings;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CopyFeatureWithSeveralConflicts {
	private String seasonID;
	private String seasonID2;
	private String stringID;
	private String featureID;
	private String configID;
	private String configID2;
	private String filePath;
	private String stringID2;
	private StringsRestApi stringsApi;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected TranslationsRestApi translationsApi;
	private String productID;
	private String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private FeaturesRestApi f;
	private UtilitiesRestApi u;
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		stringsApi = new StringsRestApi();
		stringsApi.setURL(m_translationsUrl);
        translationsApi = new TranslationsRestApi();
        translationsApi.setURL(translationsUrl);

		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}

	
	//create feature with 3 strings - conflict in value, conflict in variant, new string
	
	@Test (description = "Add string and feature with configuration rule using this string")
	public void addStrings() throws Exception{
		//key app.hello
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		//key app.hi
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);
	
			//add feature with strings to season2
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season " + featureID);
		
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration);
		configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season: " + configID);
		
		String config2 =  "{ \"text\" :  translate(\"app.hi\", \"param1\", \"param2\")	}" ;
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR2 = new JSONObject(configuration2);
		jsonCR2.put("name", "CR2");
		jsonCR2.put("configuration", config2);
		configID2 = f.addFeature(seasonID, jsonCR2.toString(), featureID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season: " + configID2);
		
		//add season2 - it will include string1, string2, string3 and feature wtih 2 configurations
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Season2 was not created: " + seasonID2);

		


	}


	@Test (dependsOnMethods="addStrings", description = "Add configuration with string3")
	public void addNewConfiguration() throws Exception{
		//key app.newhello
		String str = FileUtils.fileToString(filePath + "strings/string6.txt", "UTF-8", false);
		String stringID3 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID3.contains("error"), "String3 was not added " + stringID3);
		
		String config3 =  "{ \"text\" :  translate(\"app.newhello\")	}" ;
		String configuration3 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR3 = new JSONObject(configuration3);
		jsonCR3.put("name", "CR3");
		jsonCR3.put("configuration", config3);
		String configID3 = f.addFeature(seasonID, jsonCR3.toString(), featureID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season: " + configID3);


	}
	
/*	
	@Test (dependsOnMethods="addNewConfiguration", description = "Copy feature to season2 with 2 conflicts and 1 new string")
	public void copyFeatureDifferentVariant() throws Exception{
		String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJson = new JSONObject(str);
		strJson.put("variant", "Hello");
		String response = stringsApi.updateString(stringID, strJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String1 was not updated in season1: " + response);
		
		String str2 = stringsApi.getString(stringID2, sessionToken);
		JSONObject strJson2 = new JSONObject(str2);
		strJson2.put("value", "NewHi [[[1]]]");
		String response2 = stringsApi.updateString(stringID2, strJson2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String2 was not updated in season1: " + response);

		
		String rootId2 = f.getRootId(seasonID2, sessionToken);	
		response = f.copyFeature(featureID, rootId2, "ACT", null, "suffix1", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject json  =new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("stringsInConflict").size()==2, "Strings in conflict incorrect");
		
		//get string variant
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);	
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==3, "String3 was not copied to season2");

	}
	
*/
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
