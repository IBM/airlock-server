package tests.restapi.scenarios.strings;


import org.apache.wink.json4j.JSONObject;

import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;

public class AddAndValidateStringValue {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	
	@Test (description = "Add string value")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);

	}
	
	@Test (dependsOnMethods="addString",  description = "Check that the string was added to a list of all strings in season")
	public void validateStringInSeason() throws Exception{
		
		String response = t.getAllStrings(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray allStrings = json.getJSONArray("strings");
		Assert.assertTrue(allStrings.length()==1, "String was not added to a list of all strings in season");
		
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		
		JSONObject firstString = (JSONObject)allStrings.get(0);
		Assert.assertTrue(firstString.getString("key").equals(jsonStr.getString("key")), "Incorrect string key");
		Assert.assertTrue(firstString.getString("value").equals(jsonStr.getString("value")), "Incorrect string value");
	}
	
	@Test (dependsOnMethods="validateStringInSeason",  description = "Check mode validate in update")
	public void modeValidate() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		String response = t.addString(seasonID, str, "VALIDATE", sessionToken);
		Assert.assertTrue(response.contains("error"), "Validation correct");
		
		JSONObject jsonStr =new JSONObject(t.getString(stringID, sessionToken));
		String origValue = jsonStr.getString("value");
		jsonStr.put("value", "test hello");
		response = t.updateString(stringID, jsonStr.toString(), "VALIDATE", sessionToken);
		Assert.assertFalse(response.contains("error"));
		
		jsonStr = new JSONObject(t.getString(stringID, sessionToken));
		Assert.assertTrue(jsonStr.getString("value").equals(origValue), "String value was changed in mode VALIDATE");
	}
	
	
	@Test (dependsOnMethods="modeValidate",  description = "Check that the string was added to a list of all strings in translations")
	public void validateStringInTranslations() throws Exception{
		
		TranslationsRestApi trans = new TranslationsRestApi();
		trans.setURL(m_translationsUrl);
		String allTranslations = trans.getTranslation(seasonID, "en", "DEVELOPMENT", sessionToken);
		JSONObject jsonTrans = new JSONObject(allTranslations);
		JSONObject allStrings = jsonTrans.getJSONObject("strings");
				
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		
		Assert.assertTrue(allStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
		Assert.assertTrue(allStrings.getString(jsonStr.getString("key")).equals(jsonStr.getString("value")), "The string value was not found in translations");
	}
	
	
	@Test(dependsOnMethods="validateStringInTranslations", description="Get strings by id")
	public void getStringsById() throws Exception{
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		String stringID2 = t.addString(seasonID, str, sessionToken);
		
		String response = t.getAllStrings(seasonID, "INCLUDE_TRANSLATIONS", new String[]{stringID2}, sessionToken);
		JSONObject strings = new JSONObject(response);
		Assert.assertTrue(strings.getJSONArray("strings").size()==1, "Incorrect response in getAllStrings with id");

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
