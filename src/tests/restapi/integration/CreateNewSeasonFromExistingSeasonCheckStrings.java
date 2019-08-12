package tests.restapi.integration;

import java.util.Iterator;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;

public class CreateNewSeasonFromExistingSeasonCheckStrings {
	protected String seasonID1;
	protected String seasonID2;
	protected String productID;
	protected String config;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected StringsRestApi t;
	protected TranslationsRestApi trans;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;

		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		t = new StringsRestApi();
		trans = new TranslationsRestApi();
		p.setURL(url);
		s.setURL(url);

		t.setURL(translationsUrl);
		trans = new TranslationsRestApi();
		trans.setURL(translationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	
	}
	
	//
	@Test (description = "Add product, season and ")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID1.contains("error"), "Test should pass, but instead failed: " + seasonID1 );

		String str1 = FileUtils.fileToString(config + "/strings/string1.txt", "UTF-8", false);
		String response = t.addString(seasonID1, str1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		String str2 = FileUtils.fileToString(config + "/strings/string2.txt", "UTF-8", false);
		response = t.addString(seasonID1, str2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

		
	/* create a new season - it should copy the previous season
	 * validate that 2 strings exist in the new season
	 * 
	 */
	

	@Test (dependsOnMethods="addComponents", description = "Create new season and check its strings")
	public void createNewSeason() throws Exception{
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Test should pass, but instead failed: " + seasonID2 );
		
		String oldStrings = t.getAllStrings(seasonID1, sessionToken);
		String newStrings = t.getAllStrings(seasonID2, sessionToken);
		
		JSONObject oldJson = new JSONObject(oldStrings);
		JSONObject newJson = new JSONObject(newStrings);
		
		JSONArray oldStringsArr = oldJson.getJSONArray("strings");
		JSONArray newStringsArr = newJson.getJSONArray("strings");
		
		Assert.assertTrue(oldStringsArr.size() == newStringsArr.size(), "The number of strings in seasons is different.");
		boolean compare = compareUtilities(oldStringsArr, newStringsArr);
		Assert.assertTrue(compare, "The strings are different in 2 seasons");
		
		season2 = s.getSeason(productID, seasonID2, sessionToken);
		//JSONObject season2Json = new JSONObject(season2);
		
		//minAppVersion is removed from strings 
		//Assert.assertTrue(newStringsArr.getJSONObject(0).getString("minAppVersion").equals(oldStringsArr.getJSONObject(0).getString("minAppVersion")), "The new season string's minAppVersion is different from the old season string's minAppVersion ");
		
	}
	
	@Test (dependsOnMethods="createNewSeason", description = "Compare translations in 2 seasons")
	public void validateTranslations() throws Exception{
		String translations1 = trans.getTranslation(seasonID1, "en", "DEVELOPMENT", sessionToken);
		String translations2 = trans.getTranslation(seasonID2, "en", "DEVELOPMENT", sessionToken);
		
		JSONObject oldStrings = new JSONObject(translations1).getJSONObject("strings");
		JSONObject newStrings = new JSONObject(translations2).getJSONObject("strings");
		
	    Iterator<?> keys = oldStrings.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String) keys.next();
	        Assert.assertTrue(newStrings.containsKey(key) && newStrings.getString(key).equals(oldStrings.getString(key)), "Key/value pair incorrect for key: " + key);

	    }
	}
	
	private boolean compareUtilities(JSONArray oldSchemasArr, JSONArray newSchemasArr) throws JSONException{
		boolean compare = true;
		for (int i= 0; i < oldSchemasArr.size(); i++){
			if (!oldSchemasArr.getJSONObject(i).getString("key").equals(newSchemasArr.getJSONObject(i).getString("key")))
				compare = false;
			
			if (!oldSchemasArr.getJSONObject(i).getString("value").equals(newSchemasArr.getJSONObject(i).getString("value")))
				compare = false;
			
		}
		
		return compare;
	}
	
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
