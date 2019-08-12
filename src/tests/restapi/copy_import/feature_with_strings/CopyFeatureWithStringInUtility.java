package tests.restapi.copy_import.feature_with_strings;

import java.io.File;

import java.io.StringReader;
import java.util.Properties;

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

public class CopyFeatureWithStringInUtility {
	private String seasonID;
	private String seasonID2;
	private String featureID;
	private String configID;
	private String configID2;
	private String filePath;
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
	
	@Test (description = "Add utility and create season2")
	public void addUtility() throws Exception{
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function A(placeholder){return translate(\"app.hello\", placeholder);}");
		utilProps.setProperty("stage", "DEVELOPMENT");
		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );

		
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);

   
	}
	
	@Test (dependsOnMethods="addUtility", description = "Add string and feature with configuration rule using this string")
	public void addFeature() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		String stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		String stringID2 = stringsApi.addString(seasonID, str, sessionToken);
	
     
         
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season " + featureID);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", "{\"text\": A(\"inputString\")}");
		configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season: " + configID);
		
		String config2 =  "{ \"text\" :  translate(\"app.hi\", \"param1\", \"param2\")	}" ;
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR2 = new JSONObject(configuration2);
		jsonCR2.put("name", "CR2");
		jsonCR2.put("configuration", config2);
		configID2 = f.addFeature(seasonID, jsonCR2.toString(), featureID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season: " + configID2);

		
	}
	

	@Test (dependsOnMethods="addFeature", description = "Copy feature to season2 - no string conflict")
	public void copyFeatureDifferentSeason() throws Exception{
		String rootId2 = f.getRootId(seasonID2, sessionToken);
		String response = f.copyFeature(featureID, rootId2, "ACT", null, null, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		//validate that strings were copied to season2
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "Strings were not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hi"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hello"), "Incorrect key2 copied to season2");
		
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
