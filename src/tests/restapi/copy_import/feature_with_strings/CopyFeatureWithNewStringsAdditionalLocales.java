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

public class CopyFeatureWithNewStringsAdditionalLocales {
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
				
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);

	}
	
	/*
	 * copy feature with single string in configuration rule, no conflict, but there is additional locale and string status changes
	 */
	
	@Test (description = "Add string and feature with configuration rule using this string")
	public void addComponents() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);
	
		
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


	}
	

	@Test (dependsOnMethods="addComponents", description = "Add locales and translations to season1")
	public void addTranslations() throws Exception{
		
		//add 2 locales
		String  response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
		response = translationsApi.addSupportedLocales(seasonID,"es",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add spanish");
        //mark for translation
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark string ");
        //review for translation
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not review string ");
        //send to translation
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send string");
        //update translation
        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        String translationEs = FileUtils.fileToString(filePath + "strings/translationES.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"es",translationEs,sessionToken);
        
        String string1 = stringsApi.getString(stringID, "INCLUDE_TRANSLATIONS", sessionToken);
        JSONObject string1Json = new JSONObject(string1);
        Assert.assertTrue(string1Json.getString("status").equals("TRANSLATION_COMPLETE"), "Incorrect status in translated string1 in season1");
        
        String string2 = stringsApi.getString(stringID2, "INCLUDE_TRANSLATIONS", sessionToken);
        JSONObject string2Json = new JSONObject(string2);
        Assert.assertTrue(string2Json.getString("status").equals("TRANSLATION_COMPLETE"), "Incorrect status in translated string2 in season1");

	}

	@Test (dependsOnMethods="addTranslations", description = "Add new locale to season2")
	public void addLocale() throws Exception{
		
		//add new  locale to season2
		String response = translationsApi.addSupportedLocales(seasonID2,"de",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add german to new season");
	}

	
	
	@Test (dependsOnMethods="addLocale", description = "Copy feature to season2 without conflict, but with additional locale - status should change")
	public void copyFeatureDifferentSeason2() throws Exception{
		
		String rootId2 = f.getRootId(seasonID2, sessionToken);
		String response = f.copyFeature(featureID, rootId2, "ACT", null, null, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("addedStrings").size()==2, "Incorrect number of strings in addedStrings array");
		
		//validate that strings were copied to season2 and its status was changed
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("status").equals("IN_TRANSLATION"), "String1 status was not updated in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("status").equals("IN_TRANSLATION"), "String2 status was not updated in season2");


	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
