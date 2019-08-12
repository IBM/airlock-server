package tests.restapi.scenarios.translation;


import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;
import tests.restapi.RuntimeRestApi.DateModificationResults;

public class AddLocaleFromLocale {
	protected String seasonID;
	protected String stringID;
	protected String stringID2;
	protected String stringID3;
	protected String filePath;
	protected String str;
	protected String str2;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	TranslationsRestApi trans;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String translationsUrl;
	//private String MISSING_TRANSLATION = Strings.translationDoesNotExist;
	private FeaturesRestApi f;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String tUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		translationsUrl = tUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);
		f = new FeaturesRestApi();
	}

	@Test (description = "Add string value")
	public void addString() throws Exception{
		str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
		str2 = FileUtils.fileToString(filePath + "/strings/string2.txt", "UTF-8", false);
		stringID2 =t.addString(seasonID, str2, sessionToken);
	}

	@Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
	public void validateStringInSeason() throws Exception{
		String response = t.getAllStrings(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray allStrings = json.getJSONArray("strings");
		Assert.assertTrue(allStrings.length()==2, "String was not added to a list of all strings in season");

		//check first string is in
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		JSONObject firstString = (JSONObject)allStrings.get(0);
		Assert.assertTrue(firstString.getString("key").equals(jsonStr.getString("key")), "Incorrect string key");
		Assert.assertTrue(firstString.getString("value").equals(jsonStr.getString("value")), "Incorrect string value");

		//check second string is in
		str2 = t.getString(stringID2, sessionToken);
		JSONObject jsonStr2 = new JSONObject(str2);
		JSONObject secondString = (JSONObject)allStrings.get(1);
		Assert.assertTrue(secondString.getString("key").equals(jsonStr2.getString("key")), "Incorrect string key");
		Assert.assertTrue(secondString.getString("value").equals(jsonStr2.getString("value")), "Incorrect string value");
	}


	@Test (dependsOnMethods="validateStringInSeason",  description = "add translations in fr and es to the strings")
	public void addTranslations() throws Exception{
		//add spanish
		String esTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackES.txt", "UTF-8", false);
		String markMessage = trans.markForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String reviewedMessage = trans.reviewForTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String sendMessage = trans.sendToTranslation(seasonID ,new String[]{stringID,stringID2},sessionToken);
		String translationMessage = trans.addTranslation(seasonID,"es",esTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//add french
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFallbackFR.txt", "UTF-8", false);
		translationMessage = trans.addTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

	}
	
	@Test (dependsOnMethods="addTranslations",  description = "add supported local from locale")
	public void AddLocaleFromLocaleTest() throws Exception{
	
		String response = trans.addSupportedLocaleFromLocale(seasonID, "fr_fr", "ff", sessionToken);
		Assert.assertTrue(response.contains("error"),"can add locale from non existing locale");
		
		response = trans.addSupportedLocaleFromLocale(seasonID, "fr_fr", "fr", sessionToken);
		Assert.assertFalse(response.contains("error"),"can not add locale from an existing locale: " + response);
		
		String allFrTranslations = trans.getTranslation(seasonID, "fr", "DEVELOPMENT", sessionToken);
		String allFrFrTranslations= trans.getTranslation(seasonID, "fr_fr", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allFrTranslations.equals(allFrFrTranslations), "dev translations are not equal");
		
		allFrTranslations = trans.getTranslation(seasonID, "fr", "PRODUCTION", sessionToken);
		allFrFrTranslations= trans.getTranslation(seasonID, "fr_fr", "PRODUCTION", sessionToken);
		Assert.assertTrue(allFrTranslations.equals(allFrFrTranslations), "prod translations are not equal");
		
		String str = FileUtils.fileToString(filePath + "/strings/string3.txt", "UTF-8", false);
		stringID3 = t.addString(seasonID, str, sessionToken);
		
		response = trans.addSupportedLocaleFromLocale(seasonID, "es_es", "es", sessionToken);
		Assert.assertFalse(response.contains("error"),"can not add locale from an existing locale: " + response);
				
		String allEsTranslations = trans.getTranslation(seasonID, "es", "DEVELOPMENT", sessionToken);
		String allEsEsTranslations= trans.getTranslation(seasonID, "es_es", "DEVELOPMENT", sessionToken);
		Assert.assertTrue(allEsTranslations.equals(allEsEsTranslations), "dev translations are not equal");
		
		allEsTranslations = trans.getTranslation(seasonID, "es", "PRODUCTION", sessionToken);
		allEsEsTranslations= trans.getTranslation(seasonID, "es_es", "PRODUCTION", sessionToken);
		Assert.assertTrue(allEsTranslations.equals(allEsEsTranslations), "prod translations are not equal");
		
		trans.overrideTranslate(stringID3, "es_es", "changed", sessionToken);
		allEsTranslations = trans.getTranslation(seasonID, "es", "DEVELOPMENT", sessionToken);
		allEsEsTranslations= trans.getTranslation(seasonID, "es_es", "DEVELOPMENT", sessionToken);
		Assert.assertFalse(allEsTranslations.equals(allEsEsTranslations), "translations are equal after chnage");
	}

	@Test (dependsOnMethods="AddLocaleFromLocaleTest",  description = "remove locale leave runtime files")
	public void removeLocaleLeaveRuntimeFiles() throws Exception{
		String dateFormat = f.setDateFormat();
		
		String response = trans.removeSupportedLocales(seasonID, "fr_fr", sessionToken);
		Assert.assertFalse(response.contains("error"),"can not delete locale : " + response);
		
		DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "fr_fr", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==403, "Runtime development strings file exists");
		DateModificationResults responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "fr_fr", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==403, "Runtime production string file exists");

		
		response = trans.removeSupportedLocaleLeaveRuntimeFiles(seasonID, "es_es", sessionToken);
		Assert.assertFalse(response.contains("error"),"can not delete locale without runtime files: " + response);
		
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "es_es", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development strings file was deleted");
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "es_es", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production string file was deleted");
	
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
